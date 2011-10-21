/*
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of ReminderBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ReminderBot.java,v 1.3 2004/05/29 19:44:30 pjm2 Exp $

*/

package org.jibble.reminderbot;

import org.jibble.pircbot.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

public class ReminderBot extends PircBot implements Runnable {

    private static final String REMINDER_FILE = "reminders.dat";

    public ReminderBot(String name) {
        loadReminders();
        setName(name);
        setAutoNickChange(true);
        dispatchThread = new Thread(this);
        dispatchThread.start();
    }

    public synchronized void onMessage(String channel, String sender, String login, String hostname, String message) {

        Pattern messagePattern = Pattern.compile("^\\s*(?i:(" + getNick() + ")?\\s*[\\:,]?\\s*remind\\s+me\\s+in\\s+(((\\d+\\.?\\d*|\\.\\d+)\\s+(weeks?|days?|hours?|hrs?|minutes?|mins?|seconds?|secs?)[\\s,]*(and)?\\s+)+)(.*)\\s*)$");
        Matcher m = messagePattern.matcher(message);
        if (m.matches()) {
            String reminderMessage = m.group(7);
            String periods = m.group(2);

            long set = System.currentTimeMillis();
            long due = set;

            try {
                double weeks = getPeriod(periods, "weeks|week");
                double days = getPeriod(periods, "days|day");
                double hours = getPeriod(periods, "hours|hrs|hour|hr");
                double minutes = getPeriod(periods, "minutes|mins|minute|min");
                double seconds = getPeriod(periods, "seconds|secs|second|sec");
                due += (weeks * 604800 + days * 86400 + hours * 3600 + minutes * 60 + seconds) * 1000;
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
            if (d < 0 || d > 1e6) {
                throw new NumberFormatException("Number too large or negative (" + d + ")");
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
                sendMessage(reminder.getChannel(), reminder.getNick() + ": You asked me to remind you " + reminder.getMessage());
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

    private Thread dispatchThread;
    private LinkedList reminders = new LinkedList();

}
