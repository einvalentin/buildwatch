==========
BuildWatch
==========

This Android app shows build status information from the Continuous Integration server [Jenkins](http://jenkins-ci.org) on your Sony SmartWatch&trade; or Smart Wireless Headset pro &mdash; though if you have neither, you'll get notifications directly on your Android device.

The latest version can be downloaded from [Google Play](https://play.google.com/store/apps/details?id=com.crowflying.buildwatch&referrer=utm_source%3Dgithub%26utm_medium%3Dreadme%26utm_content%3Dapp), and further information can be found at [buildwatch.org](http://buildwatch.org/?utm_source=github&utm_medium=readme&utm_content=app).

Jenkins pushes updates to the app via Google Cloud Messaging &mdash; this requires the [GCM Notification Plugin](https://wiki.jenkins-ci.org/display/JENKINS/GCM+Notification+Plugin).  
A setup guide is available at [buildwatch.org/setup.html](http://buildwatch.org/setup.html?utm_source=github&utm_medium=readme&utm_content=app).

Have fun and please submit pull requests if you improve the app.

Thanks,  
Valentin

Planned Features
----------------
* Because of the interface from the jenkins gcm plugin, we currently don't have rich information of the build but just the build status message which we can't parse, because it is language dependent. If stuff happens on jenkins, we should try to query the build json api to find out if the build was successful or not or who is the culprit. Of course this solution is suboptimal, because it requires TCP/IP connectivity to the jenkins server from the phone - the beauty of using GCM is that this is not required.
* The settings 'only show successfull builds' and 'vibrate continuously if I broke the build' currently do nothing because of what is mentioned above.
* We currently only insert notifications into the content provider but never delete anything. We should only keep the latest 20 notifications or so. Alternatively as a quick fix, provide a setting to clear all notifications from BuildWatch
* If you don't have a SmartWatch, the app is currently of very limited use because it only shows an android notification. Maybe we can integrate with the open source jenkins monitor app somehow to have a nicer UI.
