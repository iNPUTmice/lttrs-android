package rs.ltt.android;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.mockwebserver.MockWebServer;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.activity.SetupActivity;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.client.api.HttpJmapApiClient;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static rs.ltt.android.CustomMatchers.atPosition;
import static rs.ltt.android.CustomMatchers.withError;

@RunWith(AndroidJUnit4.class)
public class SetupTest {

    @Rule
    public ActivityScenarioRule<SetupActivity> activityRule = new ActivityScenarioRule<>(SetupActivity.class);


    private final MockWebServer mockWebServer = new MockWebServer();
    private final MockMailServer mockMailServer = new MockMailServer(128);
    private final OkHttp3IdlingResource okHttp3IdlingResource = OkHttp3IdlingResource.create("OkHttp", Services.OK_HTTP_CLIENT_LOGGING);

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
    public void setupAndRefresh() throws InterruptedException {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.header)).check(matches(withText("Info required")));

        onView(withId(R.id.url)).perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.next)).perform(click());

        Thread.sleep(3000);

        intended(hasComponent(LttrsActivity.class.getName()));

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Mary Smith")))));

        mockMailServer.generateEmailOnTop();

        onView(withId(R.id.swipe_to_refresh)).perform(swipeDown());

        Thread.sleep(3000);

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Sandra Anderson")))));
    }

    @Test
    public void setupAndArchive() throws InterruptedException {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.email_address)).perform(pressImeActionButton());
        Thread.sleep(1000);
        //onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.url)).perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.url)).perform(pressImeActionButton());
        //onView(withId(R.id.next)).perform(click());
        Thread.sleep(1000);

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.password)).perform(pressImeActionButton());
        //onView(withId(R.id.next)).perform(click());

        Thread.sleep(5000);

        intended(hasComponent(LttrsActivity.class.getName()));

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Mary Smith")))));

        onView(withId(R.id.thread_list)).perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        Thread.sleep(1000);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        onView(withText("Archive")).perform(click());

        Thread.sleep(3000);
        
        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Mary Smith")))));
    }

    @Test
    public void drawerLayoutShowsAccount() throws InterruptedException {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.header)).check(matches(withText("Info required")));

        onView(withId(R.id.url)).perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.next)).perform(click());

        Thread.sleep(3000);

        intended(hasComponent(LttrsActivity.class.getName()));

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        onView(withId(R.id.navigation))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText(mockMailServer.account.getName())))));
    }

    @Test
    public void invalidManuallyEnteredEndpoint() {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.url)).perform(typeText(mockWebServer.url("not-found").toString()));
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.url_input_layout)).check(matches(withError(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.endpoint_not_found)
        )));


    }

    @Test
    public void invalidEmailAddress() {
        onView(withId(R.id.email_address)).perform(typeText("incomplete"));
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.email_address_input_layout)).check(matches(withError(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.enter_a_valid_email_address)
        )));
    }

    @Test
    public void wrongPassword() {
        onView(withId(R.id.email_address)).perform(typeText(mockMailServer.getUsername()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.url)).perform(typeText(mockWebServer.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));

        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText("wrong!"));

        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password_input_layout)).check(matches(withError(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getString(R.string.wrong_password)
        )));
    }

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