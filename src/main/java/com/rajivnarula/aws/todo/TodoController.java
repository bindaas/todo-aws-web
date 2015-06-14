package com.rajivnarula.aws.todo;

import java.util.* ;
import java.sql.Timestamp ;
import java.math.BigDecimal;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.* ;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.* ;

import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.*;

import com.wordnik.swagger.annotations.*;

@RestController
@Api( value = "/todo", description = "TODO manager" )
public class TodoController {

static DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient(new InstanceProfileCredentialsProvider()));
String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/673411742439/TODO" ;

	@RequestMapping(value = "/todo", method = { RequestMethod.POST })
    public String registerTask(@RequestParam(value = "task", required = true) String task, @RequestParam(value = "user_id", required = true) String userId) {
		long uuid = createTask (task) ;
		createUser(userId, uuid);
     	return "task->"+task;
    }

	private long createTask (String task){
		Date date= new Date();
		long timeStamp = new Timestamp(date.getTime()).getTime();
		Table table = dynamoDB.getTable("TODO");
		Item item = new Item();
		item.withPrimaryKey("UUID", new Long(timeStamp));
		item.withString("task",task);
		table.putItem(item);
		return timeStamp;
	}

	private void createUser(String user,long timeStamp){
		Table table = dynamoDB.getTable("USER");
		Item item = null ;

		item = table.getItem("user_id", user,  "task", null);
		if (item==null){
			System.out.println("createUser->NEW");
			item = new Item();
			item.withPrimaryKey("user_id", user);
			item.withNumberSet("task",timeStamp);
			table.putItem(item);
		}else{
			System.out.println("createUser->UPDATE");
			Set currentTasks = item.getNumberSet("task");
			currentTasks.add(new Long(timeStamp));
			item.withPrimaryKey("user_id", user);
			item.withNumberSet("task",currentTasks);
			table.putItem(item);
		}
	}



	@RequestMapping(value = "/todo/user/{user_id}/", method = { RequestMethod.GET })
    public String getTaskList(@PathVariable(value = "user_id") String userId , @RequestParam(value = "email", required = false) String email) {
		if ((email != null)&&("yes".equalsIgnoreCase(email))){
			AmazonSQS sqs = new AmazonSQSClient(new InstanceProfileCredentialsProvider().getCredentials());
			sqs.sendMessage(new SendMessageRequest(QUEUE_URL, userId));
			return "request submitted for ->"+userId;
		}else{
			return "task list->"+getTaskList (userId);
		}

    }

	private Set<String> getTaskList (String user){

		Table userTtable = dynamoDB.getTable("USER");
		Item userItem = null ;
		userItem = userTtable.getItem("user_id", user,  "task", null);
		Set<BigDecimal> currentTasks =null;
		if (userItem==null){
			throw new RuntimeException("User not found:"+user);
		}else{
			Set<String> listOfTasks = new HashSet<String>();
			currentTasks = userItem.getNumberSet("task");
			Table todoTable = dynamoDB.getTable("TODO");
			for (BigDecimal task :currentTasks){
				Item todoItem = todoTable.getItem("UUID", task,  "task", null);
				listOfTasks.add(todoItem.getString("task"));
			}
			return listOfTasks;
		}
	}

}