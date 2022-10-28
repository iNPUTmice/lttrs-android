# Ltt.rs for Android
[![Apache 2.0 License](https://img.shields.io/github/license/inputmice/lttrs-android?color=informational)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))
![Build Status](https://github.com/inputmice/lttrs-android/actions/workflows/android.yml/badge.svg)
[![Codacy Badge](https://img.shields.io/codacy/grade/5eac6b045963462abc5ea7ee12998353?logo=codacy)](https://www.codacy.com/gh/iNPUTmice/lttrs-android/dashboard)
[![Weblate project translated](https://img.shields.io/weblate/progress/ltt-rs)](https://hosted.weblate.org/engage/ltt-rs/)
[![Liberapay patrons](https://img.shields.io/liberapay/patrons/inputmice?logo=liberapay&style=flat&color=informational)](https://liberapay.com/iNPUTmice)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/inputmice?label=GitHub%20Sponsors)](https://github.com/sponsors/iNPUTmice/)


Proof of concept e-mail (JMAP) client (pronounced \"Letters\").
Makes heavy use of Android Jetpack to be more maintainable than some of the other Android e-mail clients.

![Android screenshot](https://gultsch.de/files/lttrs-android.png)

If the above screenshots don’t do enough to convince you, you can watch this
[short video on YouTube](https://www.youtube.com/watch?v=ArCuudFwJX4).

### Features, and design considerations:

* _Heavily cached_, but not fully offline capable. Ltt.rs makes use of JMAP’s great caching capabilities. However, marking a thread as read does round-trip to the server to update things such as read count. The action itself won’t get lost even if performed offline.
* Account _setup and done_. Settings invite feature creep and its friend unmaintainability.
* _Minimal dependencies_. Third party libraries are often of poor quality, and end up unmaintained. Only widely known, highly tested libraries from reputable vendors.
* _Native Autocrypt support_.
* _Based on [jmap-mua](https://github.com/iNPUTmice/jmap)_, a headless e-mail client, or a library that handles everything an e-mail client would, aside from data storage and UI. There is also [lttrs-cli](https://github.com/iNPUTmice/lttrs-cli), which uses the same library.
* _Looks to Gmail for inspiration_ in cases of uncertainty.

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

As of October 2022, the main options are:
* [Cyrus](https://github.com/cyrusimap/cyrus-imapd) which you can get as SaaS via [Fastmail](https://www.fastmail.com/).
* [Apache James](https://james.apache.org/). Follow [JAMES-2884](https://issues.apache.org/jira/browse/JAMES-2884) for more information.
* [Stalwart JMAP](https://github.com/stalwartlabs/jmap-server/)

See also: [https://jmap.io/software.html](https://jmap.io/software.html)


### Fastmail users
During setup, Ltt.rs will ask you for a connection URL.
You need to type in `https://jmap.fastmail.com/.well-known/jmap`.
Automatic discovery currently doesn’t work for Fastmail.

### Translations
Translations are managed on [Weblate](https://hosted.weblate.org/projects/ltt-rs/).
Register an account there to start translating.
