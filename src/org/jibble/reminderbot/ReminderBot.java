/*
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of ReminderBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

Substantial modifications by David Lazar.
*/

package org.jibble.reminderbot;

import org.jibble.pircbot.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

public class ReminderBot extends ConfigurablePircBot implements Runnable {

    private static final String REMINDER_FILE = "reminders.dat";

    public ReminderBot() {
        loadReminders();
        setAutoNickChange(true);
        dispatchThread = new Thread(this);
        dispatchThread.start();
    }

    public synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        Pattern messagePattern = Pattern.compile("^\\s*(?i:(" + getNick() + ")?\\s*[\\:,]?\\s*remind\\s+me\\s+in\\s+(((\\d+\\.?\\d*|\\.\\d+)\\s*(years?|y|weeks?|w|days?|d|hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)[\\s,]*(and)?\\s+)+)(.*)\\s*)$");
        Matcher m = messagePattern.matcher(message);
        if (m.matches()) {
            String reminderMessage = m.group(7);
            String periods = m.group(2);

            long set = System.currentTimeMillis();
            long due = set;

            try {
                double years = getPeriod(periods, "years|year|y"); // Julian year
                double weeks = getPeriod(periods, "weeks|week|w");
                double days = getPeriod(periods, "days|day|d");
                double hours = getPeriod(periods, "hours|hrs|hour|hr|h");
                double minutes = getPeriod(periods, "minutes|mins|minute|min|m");
                double seconds = getPeriod(periods, "seconds|secs|second|sec|s");
                due += (years * 31557600 + weeks * 604800 + days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000;
            }
            catch (NumberFormatException e) {
                sendMessage(channel, sender + ": I can't quite deal with numbers like that!");
                return;
            }

            if (due == set) {
                sendMessage(channel, "Example of correct usage: \"Remind me in 1 hour, 10 minutes to check the oven.\"  I understand all combinations of weeks, days, hours, minutes and seconds.");
                return;
            }

            Reminder reminder = new Reminder(channel, sender, reminderMessage, set, due);
            sendMessage(channel, sender + ": Okay, I'll remind you about that on " + new Date(reminder.getDueTime()));
            reminders.add(reminder);
            dispatchThread.interrupt();
        }
    }

    public double getPeriod(String periods, String regex) throws NumberFormatException {
        Pattern pattern = Pattern.compile("^.*?([\\d\\.]+)\\s*(?i:(" + regex + ")).*$");
        Matcher m = pattern.matcher(periods);
        m = pattern.matcher(periods);
        if (m.matches()) {
            double d = Double.parseDouble(m.group(1));
            if (d < 0) {
                throw new NumberFormatException("Unexpected negative number (" + d + ")");
            }
            return d;
        }
        return 0;
    }

    public synchronized void run() {
        boolean running = true;
        while (running) {

            // If the list is empty, wait until something gets added.
            if (reminders.size() == 0) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    // Do nothing.
                }
            }

            Reminder reminder = (Reminder) reminders.getFirst();
            long delay = reminder.getDueTime() - System.currentTimeMillis();
            if (delay > 0) {
                try {
                    wait(delay);
                }
                catch (InterruptedException e) {
                    // A new Reminder was added. Sort the list.
                    Collections.sort(reminders);
                    saveReminders();
                }
            }
            else {
                sendMessage(reminder.getChannel(), reminder.getNick() + ": On " + new Date(reminder.getSetTime()) + ", you asked me to remind you " + reminder.getMessage());
                reminders.removeFirst();
                saveReminders();
            }

        }
    }

    private void saveReminders() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(REMINDER_FILE)));
            out.writeObject(reminders);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            // If it doesn't work, no great loss!
        }
    }

    private void loadReminders() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(REMINDER_FILE)));
            reminders = (LinkedList) in.readObject();
            in.close();
        }
        catch (Exception e) {
            // If it doesn't work, no great loss!
        }
    }

    public synchronized void onDisconnect() {
        int reconnectDelay = 30; // seconds
        while (!isConnected()) {
            try {
                this.log("*** Attempting to reconnect to server.");
                reconnect();
            }
            catch (Exception e) {
                this.log("*** Failed to reconnect to server. Sleeping " + reconnectDelay + " seconds.");
                try {
                    Thread.sleep(reconnectDelay * 1000);
                } catch (InterruptedException ie) {
                    // ignored
                }
            }
        }
        // Now that we're connected, rejoin channels, if specified
        if (this.getConfiguration().containsKey("Channels")) {
            joinChannel(this.getConfiguration().getString("Channels"));
        }
    }

    public synchronized void onKick(String channel, String kickerNick, String kickerLogin,
      String kickerHostname, String recipientNick, String reason) {
        if (recipientNick.equalsIgnoreCase(getNick())) {
            int kickDelay = 5; // seconds
            this.log("*** Kicked from channel: " + channel);

            try {
                Thread.sleep(kickDelay * 1000);
            } catch (InterruptedException ie) {
                // ignored
            }

            joinChannel(channel);
        }
    }

    private Thread dispatchThread;
    private LinkedList reminders = new LinkedList();

}
