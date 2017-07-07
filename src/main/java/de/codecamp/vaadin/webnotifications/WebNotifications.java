package de.codecamp.vaadin.webnotifications;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.server.Extension;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonFactory;


/**
 * This is a Vaadin extension to support the (Web) Notifications API. This allows you to show
 * notifications directly on the user's desktop, regardless of whether browser or browser tab are
 * currently visible.
 * <p>
 * There are two competing specifications for this API: one from the
 * <a href="https://notifications.spec.whatwg.org/">WHATWG</a> and one from the
 * <a href="https://www.w3.org/TR/notifications/">W3C</a>. They share features, but there are
 * differences. And differing browser support on different platforms makes the situation even more
 * complicated. That's an abyss that I didn't want to fully explore.
 * <p>
 * If you just want to show notifications without getting down to the nitty-gritty of browser
 * support, the following options should be rather safe to use - at least in desktop browsers:
 * <ul>
 * <li>{@link NotificationBuilder#body(String) body}</li>
 * <li>{@link NotificationBuilder#icon(String) icon}</li>
 * <li>{@link NotificationBuilder#tag(String) tag}</li>
 * <li>{@link NotificationBuilder#onClick(Runnable) onClick}</li>
 * <li>{@link NotificationBuilder#onError(Runnable) onError}</li>
 * <li>{@link NotificationBuilder#timeout(Integer) timeout}</li>
 * <li>{@link NotificationBuilder#closeOnClick(boolean) closeOnClick}</li>
 * </ul>
 * <p>
 * This extension needs to be attached to a UI and needs to request the user's permission to show
 * notifications. However all that is taken care of automatically.
 * <p>
 * Uses <a href="https://github.com/alexgibson/notify.js">notify.js</a> by
 * <a href="http://alxgbsn.co.uk/">Alex Gibson</a>.
 */
@JavaScript({"WebNotificationsConnector.js", "notify.js"})
public class WebNotifications
  extends AbstractJavaScriptExtension
{

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /*
   * The weak references ensure there are no memory leaks, as there is no safe hook for cleaning up
   * the callbacks. It's possible that the callback is garbage collected too early, but considering
   * the usually limited lifespan of a notification, the chances for that should be slim. And I'd
   * rather deal with that than a memory leak.
   */
  private ConcurrentMap<String, WeakReference<Callbacks>> callbacks = new ConcurrentHashMap<>();


  /**
   * Registers the {@link WebNotifications} extension with the given UI.
   *
   * @param ui
   *          the UI
   */
  private WebNotifications(UI ui)
  {
    extend(ui);
    addFunction("onClickCallback", this::onClick);
    addFunction("onErrorCallback", this::onError);
    addFunction("onCloseCallback", this::onClose);
  }


  /**
   * Creates a notification and returns a {@link NotificationBuilder} to further customize the
   * notification before actually {@link NotificationBuilder#show() showing} it.
   * {@link UI#getCurrent()} is used to determine the UI.
   *
   * @param title
   *          the title of the notification
   * @return the {@link NotificationBuilder} to customize the notification before showing it
   */
  public static NotificationBuilder create(String title)
  {
    UI ui = UI.getCurrent();
    if (ui == null)
      throw new IllegalStateException("no current UI found");

    return create(ui, title);
  }

  /**
   * Creates a notification and returns a {@link NotificationBuilder} to further customize the
   * notification before actually {@link NotificationBuilder#show() showing} it.
   *
   * @param component
   *          the component that wishes to show a notification or at least the component that is
   *          used to determine the UI
   * @param title
   *          the title of the notification
   * @return the {@link NotificationBuilder} to customize the notification before showing it
   */
  public static NotificationBuilder create(Component component, String title)
  {
    Objects.requireNonNull(component, "component");
    Objects.requireNonNull(title, "title");

    UI ui = component.getUI();
    if (ui == null)
      throw new IllegalStateException("the component is not attached to a UI");

    Optional<Extension> extension =
        ui.getExtensions().stream().filter(e -> e instanceof WebNotifications).findAny();

    WebNotifications webnot;
    if (!extension.isPresent())
    {
      webnot = new WebNotifications(ui);
    }
    else
    {
      webnot = ((WebNotifications) extension.get());
    }

    return webnot.doCreate(title);
  }


  /**
   *
   * @param title
   *          the title of the notification
   * @return the {@link NotificationBuilder} to customize the notification before showing it
   */
  private NotificationBuilder doCreate(String title)
  {
    return new NotificationBuilder(title);
  }


  private void onClick(JsonArray arguments)
  {
    String notificationId = arguments.getString(0);
    WeakReference<Callbacks> ref = this.callbacks.remove(notificationId);
    if (ref == null)
      return;
    Callbacks callbacks = ref.get();
    if (callbacks == null)
      return;
    if (callbacks.onClick == null)
      return;

    callbacks.onClick.run();
  }

  private void onError(JsonArray arguments)
  {
    String notificationId = arguments.getString(0);
    WeakReference<Callbacks> ref = this.callbacks.remove(notificationId);
    if (ref == null)
      return;
    Callbacks callbacks = ref.get();
    if (callbacks == null)
      return;
    if (callbacks.onError == null)
      return;

    callbacks.onError.run();
  }

  private void onClose(JsonArray arguments)
  {
    /**
     * Considering the deprecated nature of onclose, it's not officially supported here. But as long
     * as browsers support it anyway, it's used to explicitly clean up callbacks.
     */
    String notificationId = arguments.getString(0);
    this.callbacks.remove(notificationId);
  }


  public class NotificationBuilder
  {

    // used to find registered callbacks
    private String notificationId = UUID.randomUUID().toString();

    private String title;


    private NotificationDirection dir = NotificationDirection.auto;

    private String lang = "";

    private String body = "";

    private String tag = "";

    private String image;

    private String icon;

    private String badge;

    private String sound;

    private Instant timestamp;

    private Boolean renotify = false;

    private Boolean silent = false;

    private Boolean requireInteraction = false;

    private String data;

    private Runnable onClickCallback;

    private Runnable onErrorCallback;


    private Integer timeout;

    private boolean closeOnClick = true;


    private NotificationBuilder(String title)
    {
      this.title = Objects.requireNonNull(title);
    }


    String getNotificationId()
    {
      return notificationId;
    }


    public NotificationBuilder dir(NotificationDirection dir)
    {
      this.dir = Optional.ofNullable(dir).orElse(NotificationDirection.auto);
      return this;
    }

    public NotificationBuilder lang(String lang)
    {
      this.lang = Optional.ofNullable(lang).orElse("");
      return this;
    }

    /**
     * The notification's body.
     *
     * @param body
     *          the notification's body
     * @return this notification builder
     */
    public NotificationBuilder body(String body)
    {
      this.body = Optional.ofNullable(body).orElse("");
      return this;
    }

    /**
     * <a href="https://notifications.spec.whatwg.org/#tags-example">A notification is considered to
     * be replaceable if there is a notification in the list of notifications whose tag is not the
     * empty string and equals the notification’s tag, and whose origin is same origin with
     * notification’s origin.</a>
     * <p>
     * Basically, notifications with the same tag will replace each other.
     *
     * @param tag
     *          the tag
     * @return this notification builder
     */
    public NotificationBuilder tag(String tag)
    {
      this.tag = Optional.ofNullable(tag).orElse("");
      return this;
    }


    /**
     * <a href="https://notifications.spec.whatwg.org/#image-resource">An image resource is a
     * picture shown as part of the content of the notification, and should be displayed with higher
     * visual priority than the icon resource and badge resource, though it may be displayed in
     * fewer circumstances.</a>
     * <p>
     * Supports normals protocols as well as Vaadin-specific protocols like {@code theme://}.
     *
     * @param imageUrl
     *          the image URL
     * @return this notification builder
     */
    public NotificationBuilder image(String imageUrl)
    {
      this.image = imageUrl;
      return this;
    }

    /**
     * <a href="https://notifications.spec.whatwg.org/#icon-resource">An image that reinforces the
     * notification (such as an icon, or a photo of the sender).</a>
     * <p>
     * Supports normals protocols as well as Vaadin-specific protocols like {@code theme://}.
     *
     * @param iconUrl
     *          the icon URL
     * @return this notification builder
     */
    public NotificationBuilder icon(String iconUrl)
    {
      this.icon = iconUrl;
      return this;
    }

    /**
     * <a href="https://notifications.spec.whatwg.org/#badge-resource">A badge resource is an icon
     * representing the web application, or the category of the notification if the web application
     * sends a wide variety of notifications. It may be used to represent the notification when
     * there is not enough space to display the notification itself. It may also be displayed inside
     * the notification, but then it should have less visual priority than the image resource and
     * icon resource.</a>
     * <p>
     * Supports normals protocols as well as Vaadin-specific protocols like {@code theme://}.
     *
     * @param badgeUrl
     *          the badge URL
     * @return this notification builder
     */
    public NotificationBuilder badge(String badgeUrl)
    {
      this.badge = badgeUrl;
      return this;
    }

    public NotificationBuilder timestamp(Instant timestamp)
    {
      this.timestamp = timestamp;
      return this;
    }

    public NotificationBuilder renotify(Boolean renotify)
    {
      this.renotify = renotify;
      return this;
    }

    public NotificationBuilder silent(Boolean silent)
    {
      this.silent = silent;
      return this;
    }

    public NotificationBuilder requireInteraction(Boolean requireInteraction)
    {
      this.requireInteraction = requireInteraction;
      return this;
    }

    /**
     * Sets an explicit timeout after which the notification will be programmatically closed.
     * Browsers will typically close them on their own after a while, so setting this should not be
     * necessary. I.e. this cannot increase the browser's own timeout, only beat it to it.
     *
     * @param timeout
     *          the timeout in seconds
     * @return this notification builder
     */
    public NotificationBuilder timeout(Integer timeout)
    {
      this.timeout = timeout;
      return this;
    }

    /**
     * Whether the notification should be closed when clicked. This makes notifications easier to
     * get rid of in browsers where this isn't the default behavior already.
     *
     * @param closeOnClick
     *          whether to close on click
     * @return this notification builder
     */
    public NotificationBuilder closeOnClick(boolean closeOnClick)
    {
      this.closeOnClick = closeOnClick;
      return this;
    }

    /**
     * The callback when the notification is clicked.
     *
     * @param onClickCallback
     *          the onClick callback
     * @return this notification builder
     */
    public NotificationBuilder onClick(Runnable onClickCallback)
    {
      this.onClickCallback = onClickCallback;
      return this;
    }

    Runnable getOnClickCallback()
    {
      return onClickCallback;
    }

    /**
     * The callback when the notification could not be shown for some reason.
     *
     * @param onErrorCallback
     *          the onError callback
     * @return this notification builder
     */
    public NotificationBuilder onError(Runnable onErrorCallback)
    {
      this.onErrorCallback = onErrorCallback;
      return this;
    }

    Runnable getOnErrorCallback()
    {
      return onErrorCallback;
    }


    /**
     * Shows the notification.
     */
    public void show()
    {
      if (getOnClickCallback() != null || getOnErrorCallback() != null)
      {
        callbacks.put(getNotificationId(),
            new WeakReference<>(new Callbacks(getOnClickCallback(), getOnErrorCallback())));
      }

      callFunction("show", title, toOptionsJson());
    }


    /**
     * Creates the options object that will be passed to the notification.
     *
     * @return the options object that will be passed to the notification
     */
    JsonValue toOptionsJson()
    {
      JreJsonFactory factory = new JreJsonFactory();
      JsonObject options = factory.createObject();

      // custom property used to map back to correct callbacks
      options.put("notificationId", notificationId);

      options.put("dir", dir.name());
      options.put("lang", lang);
      options.put("body", body);
      options.put("tag", tag);

      if (image != null)
        options.put("image", image);
      if (icon != null)
        options.put("icon", icon);
      if (badge != null)
        options.put("badge", badge);
      if (sound != null)
        options.put("sound", sound);

      if (timestamp != null)
        options.put("timestamp", timestamp.toEpochMilli());
      options.put("renotify", renotify);
      options.put("silent", silent);
      options.put("requireInteraction", requireInteraction);

      if (data != null)
        options.put("data", data);

      // custom options offered by notify.js
      if (timeout != null)
        options.put("timeout", timeout);
      options.put("closeOnClick", closeOnClick);

      // custom properties
      options.put("hasOnClick", onClickCallback != null);
      options.put("hasOnError", onErrorCallback != null);

      return options;
    }

  }

  public enum NotificationDirection
  {
    auto,
    ltr,
    rtl
  }

  private static class Callbacks
  {
    private Runnable onClick;

    private Runnable onError;


    private Callbacks(Runnable onClick, Runnable onError)
    {
      super();
      this.onClick = onClick;
      this.onError = onError;
    }
  }

}
