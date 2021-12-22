package rs.ltt.android;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import rs.ltt.android.ui.activity.ComposeActivity;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.activity.SetupActivity;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.common.entity.Role;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

@RunWith(AndroidJUnit4.class)
public class PreexistingMailboxTest {

    private final MockWebServer mockWebServer = new MockWebServer();
    private final MockMailServer mockMailServer =
            new MockMailServer(128) {
                @Override
                protected List<MailboxInfo> generateMailboxes() {
                    return Arrays.asList(
                            new MailboxInfo(UUID.randomUUID().toString(), "Inbox", Role.INBOX),
                            new MailboxInfo(
                                    UUID.randomUUID().toString(),
                                    "Archive",
                                    null), // no role -> assignment screen
                            new MailboxInfo(
                                    UUID.randomUUID().toString(),
                                    "Drafts",
                                    Role.SENT) // wrong role -> reassignment screen
                            );
                }
            };
    private final OkHttp3IdlingResource okHttp3IdlingResource =
            OkHttp3IdlingResource.create("OkHttp", Services.OK_HTTP_CLIENT);

    @Rule
    public ActivityScenarioRule<SetupActivity> activityRule =
            new ActivityScenarioRule<>(SetupActivity.class);

    @Before
    public void startServer() throws IOException {
        mockMailServer.setAdvertiseWebSocket(false);
        mockWebServer.setDispatcher(mockMailServer);
        mockWebServer.start();
        Intents.init();
    }

    @Before
    public void registerIdlingResources() {
        IdlingRegistry.getInstance().register(okHttp3IdlingResource);
    }

    @Test
    public void setupAndSwipePreexistingArchive() throws InterruptedException {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.email_address)).perform(pressImeActionButton());
        Thread.sleep(1000);
        // onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.url))
                .perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.url)).perform(pressImeActionButton());
        // onView(withId(R.id.next)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.password)).perform(pressImeActionButton());
        // onView(withId(R.id.next)).perform(click());

        Thread.sleep(5000);

        intended(hasComponent(LttrsActivity.class.getName()));
        onView(withId(R.id.thread_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        Thread.sleep(5000);

        // TODO check title

        onView(withText("Confirm")).perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.swipe_to_refresh)).perform(swipeDown());
    }

    @Test
    public void setupAndDraft() throws InterruptedException {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.url))
                .perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.next)).perform(click());

        Thread.sleep(2000);

        intended(hasComponent(LttrsActivity.class.getName()));

        onView(withId(R.id.compose)).perform(click());
        intended(hasComponent(ComposeActivity.class.getName()));

        onView(withId(R.id.subject)).perform(typeText("Hello word!"));
        closeSoftKeyboard();
        pressBack();

        // TODO check title

        Thread.sleep(3000);
    }

    // TODO check the same for sending directly (this will use a different worker)

    @After
    public void stopServer() throws IOException {
        mockWebServer.shutdown();
        Intents.release();
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(okHttp3IdlingResource);
    }
}
