package geonote.app.Note;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.ArrayList;

import geonote.app.Constants;
import geonote.app.Droplet.Model.Droplet;
import geonote.app.Tasks.GetDropletTask;
import geonote.app.Tasks.SaveDropletTask;

public class NotesManager {

    public OnNotesLoadedListener mOnNotesLoadedListener;

    public static interface OnNotesLoadedListener {
        void onNotesLoaded();
    }

    public static void loadNotesFromLocalStore(Activity activity, final NotesRepository notesRepository) {

        if (activity == null) {
            return;
        }

        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        loadNotesFromLocalStore(settings, notesRepository);
    }

    public static void loadNotesFromLocalStore(SharedPreferences settings, final NotesRepository notesRepository) {

        final String settingJson = settings.getString(Constants.PREFS_NOTES_VALUES_JSON, "");
        final Integer notesVersion = settings.getInt(Constants.PREFS_NOTES_VERSION, 0);

        // no user logged in, load the version from local disk.
        System.out.println("LoadNotes: Loading notes from local machine.");
        //System.out.println("LoadNotes: called like so " + Arrays.toString(Thread.currentThread().getStackTrace()));
        notesRepository.deserializeFromJson(settingJson, notesVersion);
    }

    public void loadNotes(final Activity activity, final NotesRepository notesRepository, final String userName) {

        if (activity == null){
            return;
        }

        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        final String settingJson = settings.getString(Constants.PREFS_NOTES_VALUES_JSON, "");
        final Integer notesVersion = settings.getInt(Constants.PREFS_NOTES_VERSION, 0);

        System.out.println("LoadNotes: looking for logged in user");
        // If the user is logged in, lets also send these notes to the droplet server
        // and associate it with the logged in user.
        if (userName != null && userName != "") {

            System.out.println("LoadNotes: Found logged in user");
            ArrayList<Droplet> droplets = new ArrayList<>();

            new GetDropletTask() {
                @Override
                protected void onPostExecute(Droplet result) {
                    final Integer notesVersionOnServer = getNotesVersion(result);

                    System.out.println("LoadNotes: notesVersionOnServer " + notesVersionOnServer);
                    System.out.println("LoadNotes: notesVersionLocal " + notesVersion);

                    if (notesVersionOnServer > notesVersion) {
                        System.out.println("LoadNotes: getting notes from the cloud");
                        new GetDropletTask() {
                            @Override
                            protected void onPostExecute(Droplet result) {
                                System.out.println("LoadNotes: got notes from the cloud, loading repository");
                                notesRepository.deserializeFromJson(result.Content, notesVersionOnServer);

                                // also bring the notes from the cloud to the local machine.
                                // the rest of the views can use this data.
                                commitNotesLocally(activity, notesRepository);

                                onRepositoryLoaded();
                            }
                        }.execute(new GetDropletTask.GetDropletTaskParam(userName, "notes"));
                    }
                    else {
                        // the version in the local machine is more advanced. Load that instead
                        // and queue a work item to update the cloud version.
                        notesRepository.deserializeFromJson(settingJson, notesVersion);
                        onRepositoryLoaded();
                        saveNotesToCloud(userName, settingJson, notesVersion);
                    }
                }
            }.execute(new GetDropletTask.GetDropletTaskParam(userName, "notes-version"));
        }
        else {
            // no user logged in, load the version from local disk.
            System.out.println("LoadNotes: Loading notes from local machine.");
            //System.out.println("LoadNotes: called like so " + Arrays.toString(Thread.currentThread().getStackTrace()));
            notesRepository.deserializeFromJson(settingJson, notesVersion);

            onRepositoryLoaded();
        }
    }

    private Integer getNotesVersion(Droplet result) {
        Integer notesVersionOnServerTmp = 0;

        if (result == null) {
            // result can be null if the server did not respond, or
            // if the version did not ever exist.
            // assume that the server version is older,
            // and proceed to load from the local version
            System.out.println("LoadNotes: Cloud not get any results for the notes-version");
        } else {
            notesVersionOnServerTmp = Integer.parseInt(result.Content);
        }
        return notesVersionOnServerTmp;
    }

    public void commitNotes(final Activity activity, final NotesRepository notesRepository, final String userName) {

        final int notesVersion = notesRepository.NotesVersion;

        if (userName != null && userName != "") {

            System.out.println("LoadNotes: Found logged in user");

            new GetDropletTask() {
                @Override
                protected void onPostExecute(Droplet result) {
                    final Integer notesVersionOnServer = getNotesVersion(result);

                    System.out.println("LoadNotes: notesVersionOnServer " + notesVersionOnServer);
                    System.out.println("LoadNotes: notesVersionLocal " + notesVersion);

                    if (notesVersionOnServer > notesVersion) {
                        System.out.println("Not Saving Notes locally or into the cloud. The version in the cloud is higher than what we have locally!");
                    } else {

                        commitNotesLocally(activity, notesRepository);

                        // If the user is logged in, lets also send these notes to the droplet server
                        // and associate it with the logged in user.
                        saveNotesToCloud(userName, notesRepository.serializeToJson(), notesVersion);
                    }
                }
            }.execute(new GetDropletTask.GetDropletTaskParam(userName, "notes-version"));
        } else {
            commitNotesLocally(activity, notesRepository);
        }
    }

    private void commitNotesLocally(Activity activity, NotesRepository notesRepository)
    {
        // increment the version of the notes every time we commit
        notesRepository.NotesVersion++;

        SharedPreferences settings = activity.getSharedPreferences(Constants.PREFS_NOTES, 0);

        String notesJson = notesRepository.serializeToJson();
        Integer notesVersion = notesRepository.NotesVersion;
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(Constants.PREFS_NOTES_VALUES_JSON, notesJson);
        editor.putInt(Constants.PREFS_NOTES_VERSION, notesVersion);

        // Commit the edits!
        editor.commit();
    }

    private void onRepositoryLoaded() {
        if (mOnNotesLoadedListener!=null)
        {
            mOnNotesLoadedListener.onNotesLoaded();
        }
    }

    private void saveNotesToCloud(String userName, String notesJson, Integer notesVersion) {
        ArrayList<Droplet> droplets = new ArrayList<>();

        droplets.add(new Droplet("notes", notesJson));
        droplets.add(new Droplet("notes-version", notesVersion.toString()));

        new SaveDropletTask().execute(
                new SaveDropletTask.SaveDropletTaskParam(userName, droplets));
    }
}
