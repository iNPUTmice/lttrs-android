package rs.ltt.android;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import okhttp3.mockwebserver.MockWebServer;
import rs.ltt.android.ui.activity.LttrsActivity;
import rs.ltt.android.ui.activity.SetupActivity;
import rs.ltt.jmap.client.Services;
import rs.ltt.jmap.mock.server.JmapDispatcher;
import rs.ltt.jmap.mock.server.MockMailServer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static rs.ltt.android.CustomMatchers.atPosition;

@RunWith(AndroidJUnit4.class)
public class AccountSwitcherTest {

    private final OkHttp3IdlingResource okHttp3IdlingResource = OkHttp3IdlingResource.create("OkHttp", Services.OK_HTTP_CLIENT);
    private final List<MockServer> MOCK_SERVERS = Stream.of(0, 1)
            .map(i -> MockServer.create(128, i))
            .collect(Collectors.toList());
    @Rule
    public ActivityScenarioRule<SetupActivity> activityRule = new ActivityScenarioRule<>(SetupActivity.class);

    @Before
    public void startServers() {
        MOCK_SERVERS.forEach(MockServer::start);
    }


    @Test
    public void setupSetupAndSwitchBack() throws InterruptedException {
        final MockServer serverDorothy = MOCK_SERVERS.get(0);
        onView(withId(R.id.email_address)).perform(typeText(serverDorothy.mail.getUsername()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.header)).check(matches(withText("Info required")));

        onView(withId(R.id.url)).perform(typeText(serverDorothy.web.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.next)).perform(click());

        Thread.sleep(3000);

        intended(hasComponent(LttrsActivity.class.getName()));

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Mary Smith")))));


        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        onView(withId(R.id.navigation))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText(serverDorothy.mail.account.getName())))));

        onView(withId(R.id.toggle)).perform(click());

        onView(withText("Add another account")).perform(click());


        final MockServer serverEdward = MOCK_SERVERS.get(1);

        onView(withId(R.id.email_address)).perform(typeText(serverEdward.mail.getUsername()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.header)).check(matches(withText("Info required")));

        onView(withId(R.id.url)).perform(typeText(serverEdward.web.url(JmapDispatcher.WELL_KNOWN_PATH).toString()));
        onView(withId(R.id.next)).perform(click());

        onView(withId(R.id.password)).perform(typeText(JmapDispatcher.PASSWORD));
        onView(withId(R.id.next)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Thomas Jones")))));

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        onView(withId(R.id.navigation))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText(serverEdward.mail.account.getName())))));

        onView(withId(R.id.toggle)).perform(click());

        onView(withText(serverDorothy.mail.getUsername())).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.thread_list))
                .perform(scrollToPosition(0))
                .check(matches(atPosition(0, hasDescendant(withText("Mary Smith")))));

    }


    @Before
    public void registerIdlingResources() {
        IdlingRegistry.getInstance().register(okHttp3IdlingResource);
        Intents.init();
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(okHttp3IdlingResource);
        Intents.release();
    }

    @After
    public void stopServers() {
        MOCK_SERVERS.forEach(MockServer::shutdown);
    }


    private static class MockServer {
        final MockWebServer web;
        final MockMailServer mail;

        private MockServer(MockWebServer web, MockMailServer mail) {
            this.web = web;
            this.mail = mail;
        }

        public static MockServer create(final int threads, final int index) {
            final MockWebServer mockWebServer = new MockWebServer();
            final MockMailServer mockMailServer = new MockMailServer(threads, index);
            mockMailServer.setAdvertiseWebSocket(false);
            mockWebServer.setDispatcher(mockMailServer);
            return new MockServer(mockWebServer, mockMailServer);
        }

        public void start() {
            try {
                web.start();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }

        public void shutdown() {
            try {
                web.shutdown();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}