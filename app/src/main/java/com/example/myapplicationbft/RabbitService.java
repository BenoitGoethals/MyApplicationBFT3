package com.example.myapplicationbft;

import android.os.Handler;
import android.util.Log;
import com.google.gson.Gson;
import com.rabbitmq.client.*;

public class RabbitService {
    Thread subscribeThread;
    Thread publishThread;


    public RabbitService() {
        setupConnectionFactory();
    }

    protected void onDestroy() {

        publishThread.interrupt();
        subscribeThread.interrupt();
    }



  private  ConnectionFactory factory = new ConnectionFactory();

    private void setupConnectionFactory() {
        String uri = "192.168.0.123";

            factory.setAutomaticRecoveryEnabled(false);
          //  factory.setUri(uri);
        factory.setHost(uri);
        factory.setUsername("test");
        factory.setPassword("test");
    }

    public void publishToAMQP(LocationBft loc) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Connection connection = factory.newConnection();
                    Channel ch = connection.createChannel();
                    ch.confirmSelect();


                    try{
                        Gson gson = new Gson();

                        String jsonInString = gson.toJson(loc);
                        ch.exchangeDeclare("bft", "direct", true);
                        ch.queueDeclare("bft-queue", true, false, false, null);
                        ch.queueBind("bft-queue", "bft", "bft");

                        ch.basicPublish("bft", "bft", null,jsonInString.getBytes());
                        Log.d("", "[s] " + loc);
                        ch.waitForConfirmsOrDie();
                    } catch (Exception e){
                        Log.d("","ERROR ----------> " );

                        throw e;
                    }

                } catch (InterruptedException e) {
                    Log.d("", e.getLocalizedMessage() + e.getClass().getName());
                } catch (Exception e) {
                    Log.d("", "Connection broken: " + e.getMessage());
                    try {
                        Thread.sleep(5000); //sleep and then try again
                    } catch (InterruptedException e1) {
                        Log.d("", e.getLocalizedMessage() + e.getClass().getName());
                    }
                }

            }

        });

        thread.start();
    }





}
