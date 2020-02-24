package com.neu.LambdaFunc;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class EmailEvent implements RequestHandler<SNSEvent, Object> {

    static DynamoDB dynamoDB;
    static final String subject = "Recipes created by you are -";
    static String htmlBody;
    private static String textBody;
    static String token;
    static String username;
    static JSONArray recipeIds;
    private String tableName=System.getenv("table");
    private Regions region = Regions.US_EAST_1;

    public String from="";


    @Override
    public Object handleRequest(SNSEvent snsEvent, Context context) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        String domainName = System.getenv("DOMAIN_NAME");
        context.getLogger().log("Domain"+domainName);
        from = "noreply@" + domainName;

        context.getLogger().log("Invocation started: " + timestamp);
        long now = Calendar.getInstance().getTimeInMillis()/1000; // unix time
        long ttl = 30*60; // ttl set to 15 min
        long totalttl = ttl + now ;

        try {
            context.getLogger().log("Message rec "+snsEvent.getRecords().get(0).getSNS().getMessage());
            JSONObject body = new JSONObject(snsEvent.getRecords().get(0).getSNS().getMessage());
            username =  body.getString("UserEmailAddress");
            recipeIds = body.getJSONArray("RecipeID");
            context.getLogger().log("Username is "+username);
            context.getLogger().log("Recipe Ids"+recipeIds);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //context.getLogger().log("Password reset request for username: "+username);
        token = UUID.randomUUID().toString();
        context.getLogger().log("Invocation completed: " + timestamp);
        try {
            initDynamoDbClient();
            long ttlDbValue = 0;
            Item item = this.dynamoDB.getTable(tableName).getItem("id", username);
            if (item != null) {
                context.getLogger().log("Checking for timestamp");
                ttlDbValue = item.getLong("ttl");
            }

            if (item == null || (ttlDbValue < now && ttlDbValue != 0)) {
                context.getLogger().log("Checking for valid ttl");
                context.getLogger().log("ttl expired, creating new token and sending email");
                this.dynamoDB.getTable(tableName)
                        .putItem(
                                new PutItemSpec().withItem(new Item()
                                        .withString("id", username)
                                        .withString("token", token)
                                        .withLong("ttl", totalttl)));
                //loop
                StringBuilder recipeIdsforEmail = new StringBuilder();
                for (int i=0; i < recipeIds.length(); i++){
                    recipeIdsforEmail.append("https://" + domainName +  "/v1/recipie/"+recipeIds.get(i) + System.lineSeparator());
                    //recipeIdsforEmail.append(System.getProperty("line.separator"));
                }
                context.getLogger().log("Text " + recipeIdsforEmail);
                htmlBody = "<h2>Email sent from Amazon SES</h2>"
                        + "<p>The url for your the recipes created by you are as below " +
                        "Link: "+ recipeIdsforEmail + "</p>";
                context.getLogger().log("This is HTML body: " + htmlBody);

                textBody="Hello "+username+ "\n You have created the following recipes. The urls are as below \n Links : "+recipeIdsforEmail;
                //Sending email using Amazon SES client
                AmazonSimpleEmailService clients = AmazonSimpleEmailServiceClientBuilder.standard()
                        .withRegion(region).build();
                SendEmailRequest emailRequest = new SendEmailRequest()
                        .withDestination(
                                new Destination().withToAddresses(username))
                        .withMessage(new Message()
                                .withBody(new Body()
                                        .withHtml(new Content()
                                                .withCharset("UTF-8").withData(htmlBody))
                                        .withText(new Content()
                                                .withCharset("UTF-8").withData(textBody)))
                                .withSubject(new Content()
                                        .withCharset("UTF-8").withData(subject)))
                        .withSource(from);
                clients.sendEmail(emailRequest);
                context.getLogger().log("Email sent successfully to email id: " +username);

            } else {
                context.getLogger().log("ttl is not expired. New request is not processed for the user: " +username);
            }
        } catch (Exception ex) {
            context.getLogger().log("Email was not sent. Error message: " + ex.getMessage());
        }
        return null;
    }

    //creating a DynamoDB Client
    private void initDynamoDbClient() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(region)
                .build();
        dynamoDB = new DynamoDB(client);
    }
}
