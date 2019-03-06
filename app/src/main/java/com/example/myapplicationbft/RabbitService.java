package com.example.myapplicationbft;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.rabbitmq.client.*;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

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
        String uri = "192.168.0.190";

            factory.setAutomaticRecoveryEnabled(false);
          //  factory.setUri(uri);
        factory.setHost(uri);
        factory.setUsername("test");
        factory.setPassword("test");
    }

    public void publishToAMQP(LocationJson loc) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Connection connection = factory.newConnection();
                    Channel ch = connection.createChannel();
                    ch.confirmSelect();


                    try{
                        ch.queueDeclare("bft-queue", false, false, false, null);
                    //    ch.exchangeDeclare("bft", BuiltinExchangeType.DIRECT);
                        ch.basicPublish("", "bft-queue", null, loc.toString().getBytes());
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





    void subscribe(final Handler handler)
    {
        /*
    }
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                while(true) {
                    try {
                        Connection connection = factory.newConnection();
                        Channel channel = connection.createChannel();
                        channel.basicQos(1);
                        AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        channel.queueBind(q.getQueue(), "amq.fanout", "chat");
                        AMQP.Queue consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        while (true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            Log.d("","[r] " + message);
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString("msg", message);
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e1) {
                        Log.d("", "Connection broken: " + e1.getClass().getName());
                        try {
                            Thread.sleep(5000); //sleep and then try again
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
        });

        subscribeThread.start();
        */
    }
}
