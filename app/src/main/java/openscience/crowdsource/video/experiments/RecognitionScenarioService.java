package openscience.crowdsource.video.experiments;

import android.annotation.SuppressLint;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import static openscience.crowdsource.video.experiments.AppConfigService.cachedScenariosFilePath;
import static openscience.crowdsource.video.experiments.AppConfigService.externalSDCardOpensciencePath;
import static openscience.crowdsource.video.experiments.AppConfigService.getSelectedRecognitionScenarioId;
import static openscience.crowdsource.video.experiments.Utils.fileToMD5;

/**
 * @author Daniil Efremov
 */

public class RecognitionScenarioService {

    private static ArrayList<RecognitionScenario> recognitionScenarios = new ArrayList<>();
    public static final Comparator<? super RecognitionScenario> COMPARATOR = new Comparator<RecognitionScenario>() {
        @SuppressLint("NewApi")
        @Override
        public int compare(RecognitionScenario lhs, RecognitionScenario rhs) {
            return Long.compare(lhs.getTotalFileSizeBytes().longValue(), rhs.getTotalFileSizeBytes().longValue());
        }
    };


    synchronized public static void cleanupCachedScenarios() {
        File file = new File(cachedScenariosFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public static ArrayList<RecognitionScenario> getSortedRecognitionScenarios() {
        if (recognitionScenarios.isEmpty()) {
            long startReloading = new Date().getTime();
            AppLogger.logMessage("Start scenarios reloading ...");
            reloadRecognitionScenariosFromFile();
            if (recognitionScenarios.isEmpty()) {
                RecognitionScenario emptyRecognitionScenario = new RecognitionScenario();
                emptyRecognitionScenario.setTitle("Preloading...");
                emptyRecognitionScenario.setTotalFileSize("");
                emptyRecognitionScenario.setTotalFileSizeBytes(Long.valueOf(0));
                AppConfigService.updateState(AppConfigService.AppConfig.State.PRELOAD);
                recognitionScenarios.add(emptyRecognitionScenario);
                new ReloadScenariosAsyncTask().execute();
            }
            AppLogger.logMessage("Scenarios reloaded for " + (new Date().getTime() - startReloading) + " ms");
        }
        Collections.sort(recognitionScenarios, COMPARATOR); // todo it's better to do only once at init
        return recognitionScenarios;
    }

    synchronized public static RecognitionScenario getSelectedRecognitionScenario() {
        int selectedRecognitionScenarioId = getSelectedRecognitionScenarioId();
        ArrayList<RecognitionScenario> sortedRecognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();
        if (selectedRecognitionScenarioId < sortedRecognitionScenarios.size()) {
            return sortedRecognitionScenarios.get(selectedRecognitionScenarioId);
        }
        return getPreloadingRecognitionScenario();
    }

    synchronized public static RecognitionScenario getPreloadingRecognitionScenario() {
        RecognitionScenario emptyRecognitionScenario = new RecognitionScenario();
        emptyRecognitionScenario.setTitle("Preloading...");
        emptyRecognitionScenario.setTotalFileSize("");
        emptyRecognitionScenario.setTotalFileSizeBytes(Long.valueOf(0));
        return emptyRecognitionScenario;
    }

    static JSONObject loadScenariosJSONObjectFromFile() {
        File scenariosFile = new File(cachedScenariosFilePath);
        if (scenariosFile.exists()) {
            try {
                JSONObject dict = openme.openme_load_json_file(cachedScenariosFilePath);
                // contract of serialisation and deserialization is not the same so i need to unwrap here original JSON
                return dict.getJSONObject("dict");
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could not read preloaded file " + cachedScenariosFilePath);
                return null;
            }
        }
        return null;
    }

    public static void saveScenariosJSONObjectToFile(JSONObject scenariousJSON) {
        File scenariosFile = new File(cachedScenariosFilePath);
        if (scenariosFile.exists()) {
            try {
                openme.openme_store_json_file(scenariousJSON, cachedScenariosFilePath);
            } catch (JSONException e) {
                AppLogger.logMessage("ERROR could save scenarios to file " + cachedScenariosFilePath + " because of the error " + e.getLocalizedMessage());
                return;
            }
        }
    }

    private static void reloadRecognitionScenariosFromFile() {
        try {

            JSONObject scenariosJSON = loadScenariosJSONObjectFromFile();
            if (scenariosJSON == null) {
                return;
            }
            JSONArray scenarios = scenariosJSON.getJSONArray("scenarios");
            if (scenarios.length() == 0) {
                return;
            }
            recognitionScenarios.clear();
            for (int i = 0; i < scenarios.length(); i++) {
                JSONObject scenario = scenarios.getJSONObject(i);


                final String module_uoa = scenario.getString("module_uoa");
                final String dataUID = scenario.getString("data_uid");
                final String data_uoa = scenario.getString("data_uoa");

                scenario.getJSONObject("search_dict");
                scenario.getString("ignore_update");
                scenario.getString("search_string");
                JSONObject meta = scenario.getJSONObject("meta");
                String title = meta.getString("title");
                String sizeMB = "";
                Long sizeBytes = Long.valueOf(0);
                try {
                    String sizeB = scenario.getString("total_file_size");
                    sizeBytes = Long.valueOf(sizeB);
                    sizeMB = Utils.bytesIntoHumanReadable(sizeBytes);
                } catch (JSONException e) {
                    AppLogger.logMessage("Warn loading scenarios from file " + e.getLocalizedMessage());
                }

                final RecognitionScenario recognitionScenario = new RecognitionScenario();
                recognitionScenario.setModuleUOA(module_uoa);
                recognitionScenario.setDataUOA(data_uoa);
                recognitionScenario.setRawJSON(scenario);
                recognitionScenario.setTitle(title);
                recognitionScenario.setTotalFileSize(sizeMB);
                recognitionScenario.setTotalFileSizeBytes(sizeBytes);
                JSONArray files = meta.getJSONArray("files");
                if (isFilesLoaded(files)) {
                    AppLogger.logMessage("All files already loaded for scenario " + recognitionScenario.getTitle());
                    recognitionScenario.setState(RecognitionScenario.State.DOWNLOADED);
                } else {
                    AppLogger.logMessage("Files not loaded yet for scenario " + recognitionScenario.getTitle());
                    recognitionScenario.setState(RecognitionScenario.State.NEW);
                }
                recognitionScenarios.add(recognitionScenario);
            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error loading scenarios from file " + e.getLocalizedMessage());
        }
    }




    public static void startDownloading(RecognitionScenario recognitionScenario) {
        if (recognitionScenario.getState() == RecognitionScenario.State.NEW) {
            AppLogger.logMessage("Downloading started for " + recognitionScenario.getTitle() + " ...");
            new LoadScenarioFilesAsyncTask().execute(recognitionScenario);
        } else if (recognitionScenario.getState() == RecognitionScenario.State.DOWNLOADING_IN_PROGRESS) {
            // mostly debug log message
            AppLogger.logMessage("Warning: Download for " + recognitionScenario.getTitle() + " already started...");
        } else if (recognitionScenario.getState() == RecognitionScenario.State.DOWNLOADED) {
            // mostly debug log message
            AppLogger.logMessage("Warning: Download for " + recognitionScenario.getTitle() + " already complete...");
        }
    }


    public static void updateProgress(final RecognitionScenario recognitionScenario) {
        RecognitionScenarioService.Updater buttonUpdater = recognitionScenario.getButtonUpdater();
        if (buttonUpdater != null) {
            buttonUpdater.update(recognitionScenario);
        }
        RecognitionScenarioService.Updater progressUpdater = recognitionScenario.getProgressUpdater();
        if (progressUpdater!=null) {
            progressUpdater.update(recognitionScenario);
        }
    }


    public interface Updater {
        void update(RecognitionScenario recognitionScenario);
    }

    private static boolean isFilesLoaded(JSONArray files) {
        try {
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.getJSONObject(j);
                String fileName = file.getString("filename");
                String fileDir = externalSDCardOpensciencePath + file.getString("path");
                File fp = new File(fileDir);
                if (!fp.exists()) {
                    if (!fp.mkdirs()) {
                        AppLogger.logMessage("\nError creating dir (" + fileDir + ") ...\n\n");
                        return false;
                    }
                }

                final String targetFilePath = fileDir + File.separator + fileName;
                String md5 = file.getString("md5");
                String existedlocalPathMD5 = fileToMD5(targetFilePath);//todo works to long, must be optimized
                if (existedlocalPathMD5 == null || !existedlocalPathMD5.equalsIgnoreCase(md5)) {
                    return false;
                }

            }
        } catch (JSONException e) {
            AppLogger.logMessage("Error checking files for scenario");
            return false;

        }
        return true;
    }
}
