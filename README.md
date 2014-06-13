# ReminderBot'

ReminderBot' (pronounced "ReminderBot prime") is a fork of jibble.org's
[ReminderBot](http://www.jibble.org/reminderbot/). ReminderBot' is based on
[PircBot'](https://github.com/davidlazar/PircBot).

# Usage

Assuming PircBot' has been installed, ReminderBot' can be built by typing:

    $ ant

After editing `config.ini`, run ReminderBot' as follows:

    $ bin/reminderbot.sh config.ini

Once in IRC, ReminderBot' is activated by saying:

    <david> remind me in 40 minutes to pickup the laundry
    <ReminderBot> david: Okay, I'll remind you about that on Sat Feb 18 03:34:41 CST 2012
