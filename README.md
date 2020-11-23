# SimpleTSViewer

Simple, very simple! It is viewer for thingspeak channels.
Properties:
    -_Support only open channels_.
    -Support show many channels, however show at one time only one channel.
    -Tune fields from channel for show( can be set field name, it measure unit, user choose fields to show)
    -May set autoupdate mode


1. Add channel
    -On first start app the addChannel confirm be showed
    |    |    |  |   |
    |:---|:-----|:---|:---|
    |![menu content](screens/menu.png " Items : Help, Channels, AddChannel, Quit " ) | Menu item "Add channel" visible only on channels screen.<br> First action: *menu -> channels*,<br> second : *menu -> Add channel*.<br> After press on "Add channel" must be add channel confirm  | ![ add channel](screens/add_channel%20screen.png "Enter channel ID") | Enter channel ID <br>**Work only open channel** <br>it is value of Channel ID on top channel screen Thingspeak site|
    |![channel properties](screens/channelProperties.png " Need fill chennel properties") | There  you must fill channel properties. All Properties is no need to save? *In this case all fields be empty*. <br>1. Name - label on top screen for this channel<br>2. request frequency - you may define it by need autoupdate channel data.<br> Field properties consist from <br>1. Field name - it be set for labeled value this field. <br>2.Measure unit - be show after field value<br>Checkbox "whether show" if not checked - field cannot be showed on channel values screen<br>ok button - press on it may be mandatory. If not pressed - changes cannot set.<br>  After fill must press on button on top screen / It leave to confirm to save/ See next picture.  |![save confirm](screens/saveConfirm.png "check on no wrong and press ok") | |