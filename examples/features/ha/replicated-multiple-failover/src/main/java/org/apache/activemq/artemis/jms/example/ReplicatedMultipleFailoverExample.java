/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jms.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.apache.activemq.artemis.util.ServerUtil;

public class ReplicatedMultipleFailoverExample {

   private static Process server0;

   private static Process server1;

   private static Process server2;

   public static void main(final String[] args) throws Exception {
      final int numMessages = 30;

      Connection connection = null;

      InitialContext initialContext = null;

      try {
         server0 = ServerUtil.startServer(args[0], ReplicatedMultipleFailoverExample.class.getSimpleName() + "0", 0, 5000);
         server1 = ServerUtil.startServer(args[1], ReplicatedMultipleFailoverExample.class.getSimpleName() + "1", 1, 5000);
         server2 = ServerUtil.startServer(args[2], ReplicatedMultipleFailoverExample.class.getSimpleName() + "2", 2, 5000);

         Process[] processes = new Process[]{server0, server1, server2};

         // Step 1. Get an initial context for looking up JNDI from the server #1
         initialContext = new InitialContext();

         // Step 2. Look up the JMS resources from JNDI
         Queue queue = (Queue) initialContext.lookup("queue/exampleQueue");
         ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

         // Step 3. Create a JMS Connection
         connection = connectionFactory.createConnection();

         // Step 4. Create a *non-transacted* JMS Session with client acknowledgement
         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         // Step 5. Start the connection to ensure delivery occurs
         connection.start();

         // Step 6. Create a JMS MessageProducer and a MessageConsumer
         MessageProducer producer = session.createProducer(queue);
         MessageConsumer consumer = session.createConsumer(queue);

         // Step 7. Send some messages to server #1, the live server
         for (int i = 0; i < numMessages; i++) {
            TextMessage message = session.createTextMessage("This is text message " + i);
            producer.send(message);
            System.out.println("Sent message: " + message.getText());
         }

         // Step 8. Receive and acknowledge a third of the sent messages
         TextMessage message0 = null;
         for (int i = 0; i < numMessages / 3; i++) {
            message0 = (TextMessage) consumer.receive(5000);
            System.out.println("Got message: " + message0.getText());
         }
         message0.acknowledge();

         // Step 9. Receive the rest third of the sent messages but *do not* acknowledge them yet
         for (int i = numMessages / 3; i < numMessages; i++) {
            message0 = (TextMessage) consumer.receive(5000);
            System.out.println("Got message: " + message0.getText());
         }

         // Step 10. Crash server #0, the live server, and wait a little while to make sure
         // it has really crashed
         ServerUtil.killServer(server0);
         Thread.sleep(5000);

         // Step 11. Acknowledging the 2nd half of the sent messages will fail as failover to the
         // backup server has occurred
         try {
            message0.acknowledge();
         } catch (JMSException e) {
            System.err.println("Got exception while acknowledging message: " + e.getMessage());
         }

         // Step 12. Consume again the 2nd third of the messages again. Note that they are not considered as redelivered.
         for (int i = numMessages / 3; i < (numMessages / 3) * 2; i++) {
            message0 = (TextMessage) consumer.receive(5000);
            System.out.printf("Got message: %s (redelivered?: %s)%n", message0.getText(), message0.getJMSRedelivered());
         }
         message0.acknowledge();

         ServerUtil.killServer(processes[ServerUtil.getServer(connection)]);

         // Step 11. Acknowledging the 2nd half of the sent messages will fail as failover to the
         // backup server has occurred
         try {
            message0.acknowledge();
         } catch (JMSException e) {
            System.err.println("Got exception while acknowledging message: " + e.getMessage());
         }

         // Step 12. Consume again the 2nd third of the messages again. Note that they are not considered as redelivered.
         for (int i = (numMessages / 3) * 2; i < numMessages; i++) {
            message0 = (TextMessage) consumer.receive(5000);
            System.out.printf("Got message: %s (redelivered?: %s)%n", message0.getText(), message0.getJMSRedelivered());
         }
         message0.acknowledge();
      } finally {
         // Step 13. Be sure to close our resources!

         if (connection != null) {
            connection.close();
         }

         if (initialContext != null) {
            initialContext.close();
         }

         ServerUtil.killServer(server0);
         ServerUtil.killServer(server1);
         ServerUtil.killServer(server2);
      }
   }
}
