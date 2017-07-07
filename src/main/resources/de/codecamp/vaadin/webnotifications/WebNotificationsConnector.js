window.de_codecamp_vaadin_webnotifications_WebNotifications = function() {

  var self = this;

  this.requestPermission = function() {
    Notify.requestPermission();
  }

  this.show = function(title, options) {

    // translate URLs with Vaadin-specific protocols like theme:// to something
    // the browser can actually load
    if (typeof options.image !== "undefined") {
      options.image = self.translateVaadinUri(options.image);
    }
    if (typeof options.icon !== "undefined") {
      options.icon = self.translateVaadinUri(options.icon);
    }
    if (typeof options.badge !== "undefined") {
      options.badge = self.translateVaadinUri(options.badge);
    }
    if (typeof options.sound !== "undefined") {
    	options.sound = self.translateVaadinUri(options.sound);
    }

    if (options.hasOnClick) {
      options.notifyClick = function() {
        self.onClickCallback(options.notificationId);
      };
    }
    if (options.hasOnError) {
      options.notifyError = function() {
        self.onErrorCallback(options.notificationId);
      };
    }

    // deprecated but used for clean up as long as it's still supported
    options.notifyClose = function() {
      self.onCloseCallback(options.notificationId);
    };

    if (!Notify.needsPermission) {
      new Notify(title, options).show();
    } else if (Notify.isSupported()) {
      Notify.requestPermission(function onPermissionGranted() {
        new Notify(title, options).show();
      }, null);
    }
  }

}
