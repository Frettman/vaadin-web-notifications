# Web Notifications Add-on for Vaadin 8

This is a Vaadin extension to support the (Web) Notifications API. This allows you to show notifications directly on the user's desktop, regardless of whether browser or browser tab are currently visible.

There are two competing specifications for this API: one from the [WHATWG](https://notifications.spec.whatwg.org/) and one from the [W3C](https://www.w3.org/TR/notifications/). They share features, but there are differences. And differing browser support on different platforms makes the situation even more complicated. That's an abyss that I didn't want to fully explore.

If you just want to show notifications without getting down to the nitty-gritty of browser support, the following options should be rather safe to use - at least in desktop browsers:

+ body(String) body}
+ icon(String) icon}
+ tag(String) tag}
+ onClick(Runnable)
+ onError(Runnable)
+ timeout(Integer)
+ closeOnClick(boolean)

This extension needs to be attached to a UI and needs to request the user's permission to show notifications. However all that is taken care of automatically.

Uses [notify.js](https://github.com/alexgibson/notify.js) by [Alex Gibson](http://alxgbsn.co.uk/).

## Download release

Official releases of this add-on are available at Vaadin Directory. For Maven instructions, download and reviews, go to http://vaadin.com/addon/TODO

## Example

    WebNotifications.create("Title")
      .body("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aliquam hendrerit mi posuere lectus. Vestibulum enim wisi, viverra nec, fringilla in, laoreet vitae, risus.")
      .icon("theme://img/logo.png").tag("sometag")
      .onClick(() -> Notification.show("onClick")).show());

## License

Add-on is distributed under MIT License. For license terms, see LICENSE.txt.
