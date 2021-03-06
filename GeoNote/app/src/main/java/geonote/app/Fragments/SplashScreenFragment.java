package geonote.app.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Locale;

import geonote.app.Activity.MainActivity;
import geonote.app.Constants;
import geonote.app.Note.NotesManager;
import geonote.app.Note.NotesRepository;
import geonote.app.R;

public class SplashScreenFragment extends BaseFacebookHandlerFragment {

    View mCurrentView = null;
    NotesManager mNotesManager = null;
    NotesRepository mNotesRepository = null;
    TextView mSplashScreenTextView = null;
    Time startupTime = new Time();
    int splashScreenShowTimeMillis = 1*1000;

    public SplashScreenFragment() {
        startupTime.setToNow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mCurrentView = inflater.inflate(R.layout.fragment_splash_screen, container, false);

        mSplashScreenTextView = (TextView) mCurrentView.findViewById(R.id.splashScreenText);

        return mCurrentView;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        mNotesManager = new NotesManager();
        mNotesRepository = new NotesRepository(new Geocoder(getActivity().getBaseContext(), Locale.getDefault()));

        try {
            NotesRepository.SetInstance(mNotesRepository);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mNotesManager.mOnNotesLoadedListener  = new NotesManager.OnNotesLoadedListener() {
            @Override
            public void onNotesLoaded() {
                logAndShowOnScreen("\nNotes loaded.");
                launchMainActivity();
            }
        };

        Session session = Session.getActiveSession();

        if(session!= null && session.isOpened()) {
            logAndShowOnScreen("\nLogging into facebook.");
        } else {
            logAndShowOnScreen("\nNo facebook app/login info found.");
            mNotesManager.loadNotes(getActivity(), mNotesRepository, null);
        }
    }

    private void logAndShowOnScreen(String logText) {
        Log.d("SplashScreen", logText);
        mSplashScreenTextView.append(logText);
    }

    public void loadNotes(Activity activity, final String userName) {

        // TODO: Load notes only if not already loaded
        logAndShowOnScreen("\nRetrieving notes.");
        mNotesManager.loadNotes(activity, mNotesRepository, userName);
    }

    protected void onSessionStateChange(Session session, SessionState state, Exception exception){
        if (session != null && session.isOpened()) {
            logAndShowOnScreen("\nLogged in. Getting user details.");
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user,
                                        Response response) {
                    if (user != null) {
                        System.out.println("onSessionStateChange: LoadNotes: session is open. username:"+user.getName());

                        loadNotes(getActivity(), user.getId());
                    }
                }
            });
            Request.executeBatchAsync(request);
        } else {
            logAndShowOnScreen("\nUser not already logged in.");
            System.out.println("onSessionStateChange: LoadNotes: session was closed.");
            loadNotes(getActivity(), getLoggedInUsername());
        }
    }

    private void launchMainActivity() {
        logAndShowOnScreen("\nLaunching map view.");

        final Fragment currentFragment = this;
        final Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {

            @Override
            public void run() {
                while((getActivity()) == null) {
                    Log.d("SplashScreen", "Activity is null");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LaunchMainActivity(getActivity(), currentFragment);
                SplashScreenFragment.this.getActivity().finish();
            }
        };

        Time now = new Time();
        now.setToNow();
        timerHandler.postDelayed(timerRunnable,
                    startupTime.toMillis(false)
                    + splashScreenShowTimeMillis
                    - now.toMillis(false));
    }

    public static void LaunchMainActivity(Activity currentActivity, Fragment currentFragment) {
        Intent myIntent = new Intent(currentActivity, MainActivity.class);
        if (currentFragment != null) {
            currentFragment.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        } else {
            currentActivity.startActivityForResult(myIntent, Constants.ACTIVITY_NOTE_VIEW);
        }
    }
}