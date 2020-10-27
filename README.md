# Ltt.rs for Android

Proof of concept e-mail (JMAP) client (pronounced \"Letters\").
Makes heavy use of Android Jetpack to be more maintainable than some of the other Android e-mail clients.

![Android screenshot](https://gultsch.de/files/lttrs-android.png)

If the above screenshots don’t do enough to convince you, you can watch this
[short video on YouTube](https://www.youtube.com/watch?v=ArCuudFwJX4).

### Features, and design considerations:

* _Heavily cached_, but not fully offline capable. Ltt.rs makes use of JMAP’s great caching capabilities. However, marking a thread as read does round-trip to the server to update things such as read count. The action itself won’t get lost even if performed offline.
* Account _setup and done_. Settings invite feature creep and its friend unmaintainability.
* _Minimal dependencies_. Third party libraries are often of poor quality, and end up unmaintained. Only widely known, highly tested libraries from reputable vendors.
* _Native Autocrypt support_.¹
* _Based on [jmap-mua](https://github.com/iNPUTmice/jmap)_, a headless e-mail client, or a library that handles everything an e-mail client would, aside from data storage and UI. There is also [lttrs-cli](https://github.com/iNPUTmice/lttrs-cli), which uses the same library.
* _Looks to Gmail for inspiration_ in cases of uncertainty.

¹: Planned feature

### Try it

**Attention: You need a JMAP capable mail server to use Ltt.rs**

You can download Ltt.rs either from [F-Droid](https://f-droid.org/en/packages/rs.ltt.android), or
for a small fee from [Google Play](https://play.google.com/store/apps/details?id=rs.ltt.android).

If you want to use F-Droid you can also use our F-Droid repository instead of
the official one:
```
https://ltt.rs/fdroid/repo?fingerprint=9C2E57C85C279E5E1A427F6E87927FC1E2278F62D61D7FCEFDE9346E568CCF86
```

All three versions are signed with the same key, so it is possible to switch between them.

JMAP servers are currently rare. As of October 2020 you need 
[Cyrus](https://github.com/cyrusimap/cyrus-imapd) 3.2.x.
[Fastmail](https://www.fastmail.com/) is the only known provider.


### Fastmail users
During setup, Ltt.rs will ask you for a connection URL.
You need to type in `https://jmap.fastmail.com/.well-known/jmap`.
Automatic discovery currently doesn’t work for Fastmail.

### Translations
Translations are managed on [Weblate](https://hosted.weblate.org/projects/ltt-rs/).
Register an account there to start translating.
