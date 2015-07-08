package org.vertx.mods.mqtt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.paho.client.mqttv3.*;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class MQTT extends BusModBase implements Handler<Message<JsonObject>>
{
	protected String host;
    protected int port;
    protected String clientID;
    private static MqttClient mClient;
    protected String Address;
    protected JsonObject dbConfig;
    protected String deployID;
    protected String dbname;
    protected String topic;
    protected JsonObject Topic;
	protected String key;
    protected static String IV = "AAAAAAAAAAAAAAAA";
    protected int tseed1, tseed2;
    protected String checkID;
    protected String pTopic;
    protected int pSecureKey;
    
	@Override
    public void start()
    {
        super.start();
        port = getOptionalIntConfig("port", 1883);
        host = getOptionalStringConfig("host", "localhost");
        clientID = getOptionalStringConfig("clientID", MqttClient.generateClientId());
        dbConfig = getOptionalObjectConfig("dbConfig", null);
        Address = "mqttclient";
        topic = getOptionalStringConfig("topic", "g2PYYeRkm4XwNs5SkT%2BEm6ZWuLXQCBNLJ4jdEH43rTuU0WjKjo");
        Topic = new JsonObject();
        if(topic == null)
            Topic.putString("Topic", "g2PYYeRkm4XwNs5SkT%2BEm6ZWuLXQCBNLJ4jdEH43rTuU0WjKjo");
        else
            Topic.putString("Topic", topic);
        try
        {
            mClient = new MqttClient((new StringBuilder("tcp://")).append(host).append(":").append(port).toString(), clientID);
            mClient.connect();
        }
        catch(MqttException e)
        {
            e.printStackTrace();
        }
        eb.registerHandler(Address, this);
    }

    public void handle(Message<JsonObject> message)
    {
    	int length = 30;
    	for(int i = 0; i<length ; i++)
    		System.out.print("=");
    	System.out.print("Handler Called");
    	for(int i = 0; i<length ; i++)
    		System.out.print("=");
    	System.out.println("");
    	System.out.println(((JsonObject)message.body()).toString());
    	System.out.println("");
    	String action = ((JsonObject)message.body()).getString("action");
    	if(action == null)
    	{
    		sendError(message, "Error : action must be specified");
    		return;
    	}
    	switch(action)
    	{
    	case "registor": 
    		try {
				doRegistor(message);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		break;
    	case "publish":
    		doPublish(message);
    		break;
    	case "subscribe": 
    		try
    		{
    			doSubscribe(message);
    		}
    		catch(MqttException e1)
    		{
    			e1.printStackTrace();
    		}
    		break;
       	default:
        	sendError(message, (new StringBuilder("Invalid action: ")).append(action).toString());
           	break;
    	}
    }

    private void doSubscribe(Message<JsonObject> message)
        throws MqttException
    {
        String topic = getMandatoryString("topic", message);
        mClient.subscribe((new StringBuilder(String.valueOf(topic))).append("/").append(topic).toString());
        mClient.setCallback(new MqttCallback() {
            public void connectionLost(Throwable throwable)
            {
            	System.out.println("connection lost");
            }

            public void deliveryComplete(IMqttDeliveryToken imqttdeliverytoken)
            {
            }

            public void messageArrived(String arg0, MqttMessage arg1)
                throws Exception
            {
                JsonObject doc = new JsonObject(arg1.toString());
                subscribeFunc(doc, dbConfig);
            }

        });
    }

    private void doPublish(Message<JsonObject> message)
    {
        String topic = getMandatoryString("topic", message);
        if(topic == null)
            return;
        JsonObject doc = getMandatoryObject("document", message);
        if(doc == null)
            return;
        MqttMessage mMessage = new MqttMessage(doc.toString().getBytes());
        try
        {
        	MqttTopic topictest = mClient.getTopic((new StringBuilder(String.valueOf(topic))).append("/").append(topic).toString());
            MqttDeliveryToken token = topictest.publish(mMessage);
            token.waitForCompletion(1000);
        }
        catch(MqttPersistenceException e)
        {
            e.printStackTrace();
        }
        catch(MqttException e)
        {
            e.printStackTrace();
        }
    }

    private void doRegistor(Message<JsonObject> message) throws Exception
    {
    	checkID = getMandatoryString("tgID", message);
    	if(checkID == null)
    		return;
    	int secureNum = (Integer) message.body().getNumber("secureNum");
    	int seed1, seed2;
    	seed1 = secureNum/1000;
    	seed2 = secureNum%1000;
    	key = SHA256(seed1, seed2);
    	System.out.println("SHA Key : " + key);
    	System.out.println("topic : " + topic);
    	System.out.println("");
    	Random random = new Random();
    	
    	tseed1 = random.nextInt(999)+1;
    	tseed2 = random.nextInt(999)+1;
    	
    	JsonObject Message,regiMessage;
    	Message = new JsonObject();
    	regiMessage = new JsonObject();
    	System.out.println("seed Number : " + tseed1 + " " + tseed2);
    	    	
    	Message.putValue("num1", tseed1);
    	Message.putValue("num2", tseed2);
    	
    	byte[] encryptMessage;
    	
    	encryptMessage = encrypt(Message.toString(),key);
    	regiMessage.putString("type", "00");
        regiMessage.putBinary("data", encryptMessage);
        
        System.out.println("Encrypt : " + Message.toString() + " -> ");
        for (int i=0; i<encryptMessage.length; i++)
	    	System.out.print(new Integer(encryptMessage[i])+" ")	;
	    System.out.println("");
    	System.out.println("");
        
    	MqttMessage mMessage = new MqttMessage(regiMessage.toString().getBytes());
    	try
    	{
    		mClient.publish(checkID, mMessage);
    	}
    	catch(MqttPersistenceException e)
    	{
    		e.printStackTrace();
    	}
    	catch(MqttException e)
    	{
    		e.printStackTrace();
    	}
    	
    	key = SHA256(tseed1, tseed2);
    	    	
    	try {
			mClient.subscribe(checkID);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	mClient.setCallback(new MqttCallback() {
    		public void connectionLost(Throwable throwable)
    		{
    		}
    		
    		public void deliveryComplete(IMqttDeliveryToken imqttdeliverytoken)
    		{
    		}
    		
    		public void messageArrived(String arg0, MqttMessage arg1)
    				throws Exception
    		{	
    			System.out.println(arg1.toString());
    			JsonObject doc = new JsonObject(arg1.toString());
    			registerSubFunc(doc);
    		}	
    	});
    }
    
    private void registerSubFunc(final JsonObject doc) throws Exception
    {
    	if(doc.getNumber("Number")!=null)
    	{
    		return;
    	}
    	Boolean unsub = false;
		JsonObject Message,sendMessage;
    	Message = new JsonObject();
    	byte[] encryptMessage = null;
    	sendMessage = new JsonObject();
    	byte[] encryptData = doc.getBinary("data");
        
    	String Data = decrypt(encryptData,key);
    		System.out.print("decrypt : ");
	    for (int i=0; i<encryptData.length; i++)
	    	System.out.print(new Integer(encryptData[i])+" ");
	    System.out.println(" -> " + Data);
    	System.out.println("");
    	String[] Datas = Data.split(" ");
    	Thread.sleep(2000);
    	if(checkID.equalsIgnoreCase(Datas[0]))
    	{
    		System.out.println("client's Key is correct.");
        	key = SHA256(tseed1, tseed2);
    		Message.putString("topic", topic);
    		
    		encryptMessage = encrypt(Message.toString(), key);
    		unsub = true;
    		
    		System.out.println("Encrypt : " + Message.toString() + " -> " );
    	    for (int i=0; i<encryptMessage.length; i++)
    	    	System.out.print(new Integer(encryptMessage[i])+" ")	;
    	    System.out.println("");
    		sendMessage.putString("type","01");
    	}
    	else
    	{
    		String checkString = doc.getString("notice");
        	if(checkString == null)
        	{
        		System.out.println("client's Key is incorrect... resend seed number");
            	System.out.println("");
        		Message.putValue("num1", tseed1);
        		Message.putValue("num2", tseed2);
        	        	
        		encryptMessage = encrypt(Message.toString(),key);
        		sendMessage.putString("type","00");
        	}
    	}
    	
    	if(encryptMessage != null)
    	{
    		sendMessage.putBinary("data", encryptMessage);
    		
    		MqttMessage mMessage = new MqttMessage(sendMessage.toString().getBytes());
        	MqttTopic topictest = mClient.getTopic(checkID);
            MqttDeliveryToken token = topictest.publish(mMessage);
            token.waitForCompletion(1000);
    		
    		if(unsub == true)
    		{
    			System.out.println("End of Register");
    			mClient.unsubscribe(checkID);    			
    		}
    	}
    }
 
    private void subscribeFunc(final JsonObject doc, final JsonObject dbConfig)
    {
    	eb = vertx.eventBus();
    	container.deployModule("mongo-persistor", dbConfig, new Handler<AsyncResult<String>>() {
    		@Override
    		public void handle(AsyncResult<String> info) {
    			String lognum = "";
    			switch(doc.getString("type"))
    			{
    			case "log": 
    				dbname = "logdata";
    				lognum = doc.getString("log_id");
    				doc.removeField("log_id");
    				break;
    			case "sensordata": 
    				dbname = "sensordata";
    				break;
    			default:
    				System.out.println((new StringBuilder("Unknown Message: ")).append(doc.getString("Type")).toString());
    				break;
    			}
    			boolean check = (doc.getString("type")!=null);
    			doc.removeField("type");
    			if(dbConfig.containsField("db_name"))
    				dbConfig.removeField("db_name");
    			dbConfig.putString("db_name", dbname);
    			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
    			Date currentTime = new Date();
    			String dTime = formatter.format(currentTime);
    			doc.putString("time", dTime);
    			JsonObject sendData = new JsonObject();
    			if(doc.containsField("tg_id"))
    			{ // {"type":"log","log_id":"99","TG_ID":"TG01","Rack_List":[{"Rack_ID":"Rack01","Sensor_List":["Temp","Light"]}],"Act_List":[{"Act_ID":"Act03","Status":"on"},{"Act_ID":"Act02","Status":"on"}]}
    				sendData.putString("collection",doc.getString("tg_id"));
    				doc.removeField("tg_id");
    			}
    			else if(lognum!=null && lognum.equals("99"))
				{
    				sendData.putString("collection","TG03");
				}
    			else
    			{
    				sendData.putString("collection", "data");
    			}
    			
    			if(check)
    			{
        			sendData.putString("action", "save");
        			sendData.putObject("document", doc);
        			String dbname = dbConfig.getString("db_name");
        			if(lognum!=null && lognum.equals("99"))
        			{
        				dbname = "userdata";
        				doc.removeField("log_id");
        			}
        			sendData.putString("db_name", dbname);
        			System.out.println("store received Message in DB");
        			System.out.println(sendData.toString());
        			eb.send("mongo-persistor", sendData);
    			}
    		}
    		
    	});
    }
    
    public byte[] encrypt(String plainText, String encryptionKey) throws Exception {
    	plainText = MakeMultipleHexD(plainText);
    	Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
    	SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
    	cipher.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
    	return cipher.doFinal(plainText.getBytes("UTF-8"));
    }
    
    public String decrypt(byte[] cipherText, String encryptionKey) throws Exception{
    	Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
    	SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
    	cipher.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
    	return new String(cipher.doFinal(cipherText),"UTF-8");
    }
    
    public String SHA256(int num1, int num2){
    	String SHA = "12345612431512315123154123"; 
    	try{
    		MessageDigest sh = MessageDigest.getInstance("SHA-256"); 
    		String str = String.valueOf((num1+1)*(num1-1)*(num2+1)*(num2-1));
    		sh.update(str.getBytes()); 
    		byte byteData[] = sh.digest();
    		StringBuffer sb = new StringBuffer(); 
    		for(int i = 0 ; i < byteData.length ; i++){
    			sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
    		}
    		SHA = sb.toString();
    		
    	}catch(NoSuchAlgorithmException e){
    		e.printStackTrace(); 
    		SHA = null; 
    	}
    	SHA = SHA.substring(0,16);
    	return SHA;
    }
    
    public static byte[] stringToByte(String str)
    { 
    	int strLen = str.length();
    	byte[] cVal = new byte[strLen];
    	cVal = str.getBytes();
    	
    	return cVal;
    }
        
    public String MakeMultipleHexD(String input)
    {
    	int size = input.length();
    	size = size%16;
    	if(size != 0)
    		size = 16-size;
    	StringBuilder strBuf = new StringBuilder(input);
    	for(int i = 0; i<size; i++)
    	{
    		 strBuf.append(" ");
    	}
    	input = strBuf.toString();
		return input;
    }
}