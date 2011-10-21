/*
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of ReminderBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: ReminderBotMain.java,v 1.2 2004/05/29 19:27:37 pjm2 Exp $

*/

package org.jibble.reminderbot;

import java.util.*;
import java.io.*;

public class ReminderBotMain {

    public static void main(String[] args) throws Exception {

        Properties p = new Properties();
        p.load(new FileInputStream(new File("./config.ini")));

        String server = p.getProperty("Server", "localhost");
        String channel = p.getProperty("Channel", "#test");
        String nick = p.getProperty("Nick", "ReminderBot");

        ReminderBot bot = new ReminderBot(nick);
        bot.setVerbose(true);
        bot.connect(server);
        bot.joinChannel(channel);

    }

}
