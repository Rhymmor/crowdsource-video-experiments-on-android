package openscience.crowdsource.video.experiments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import static openscience.crowdsource.video.experiments.MainActivity.setTaskBarColored;

public class ScenariosActivity extends AppCompatActivity {
    private LayoutInflater inflator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scenarios);
        setTaskBarColored(this);
        inflator = LayoutInflater.from(this);

        View selectScenario = (View)findViewById(R.id.selectScenario);
        selectScenario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                startActivity(homeIntent);
            }
        });

        Button homeButton = (Button) findViewById(R.id.btn_home_recognizeMain);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                startActivity(homeIntent);
            }
        });


        Button consoleButton = (Button) findViewById(R.id.btn_consoleMain);
        consoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent logIntent = new Intent(ScenariosActivity.this, ConsoleActivity.class);
                startActivity(logIntent);
            }
        });


        Button infoButton = (Button) findViewById(R.id.btn_infoMain);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent infoIntent = new Intent(ScenariosActivity.this, InfoActivity.class);
                startActivity(infoIntent);
            }
        });


        final ViewGroup resultRadioGroup = (ViewGroup) findViewById(R.id.rgScenariosList);

        ArrayList<RecognitionScenario> sortedRecognitionScenarios = RecognitionScenarioService.getSortedRecognitionScenarios();

        if (sortedRecognitionScenarios.size() == 0) {
            AppLogger.logMessage("There is not scenarios found for your device...");
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            final TextView resultItemView = new TextView(this);
            resultItemView.setPadding(0, 20, 0 , 20);
            Spanned spanned;
            spanned = Html.fromHtml("<font color='#ffffff'><b>There is not scenarios found for your device...</b></font>");
            resultItemView.setText(spanned);
            ll.addView(resultItemView);
            resultRadioGroup.addView(ll);
        } else {

            for (int i=0; i < sortedRecognitionScenarios.size(); i++) {
                final RecognitionScenario recognitionScenario = sortedRecognitionScenarios.get(i);
                final View scenarioItemConvertView = inflator.inflate(R.layout.scenario_item, null);
                resultRadioGroup.addView(scenarioItemConvertView);
                View ll = scenarioItemConvertView.findViewById(R.id.scenario_row);
                final int selected = i;
                TextView scenarioTextView = (TextView) scenarioItemConvertView.findViewById(R.id.scenario);
                scenarioTextView.setText(recognitionScenario.getTitle());
                scenarioTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppConfigService.updateSelectedRecognitionScenario(selected);
                        Intent homeIntent = new Intent(ScenariosActivity.this, MainActivity.class);
                        startActivity(homeIntent);
                    }
                });

                final TextView volumeTextView = (TextView) scenarioItemConvertView.findViewById(R.id.volume_mb);
                volumeTextView.setText(recognitionScenario.getTotalFileSize());
                recognitionScenario.setProgressUpdater(new RecognitionScenarioService.Updater() {
                    @Override
                    public void update(final RecognitionScenario recognitionScenario) {

                        ScenariosActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                                    volumeTextView.setText(
                                            Utils.bytesIntoHumanReadable(recognitionScenario.getDownloadedTotalFileSizeBytes()) +
                                                    " of " +
                                                    Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                                    scenarioItemConvertView.refreshDrawableState();
                                } else {
                                    volumeTextView.setText(
                                            Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
                                }
                            }
                        });

                        // todo change view volumeTextView.
                    }
                });
                recognitionScenario.getProgressUpdater().update(recognitionScenario);

                final ImageView scenarioInfoButton = (ImageView) scenarioItemConvertView.findViewById(R.id.ico_scenarioInfo);
                scenarioInfoButton.setVisibility(View.VISIBLE);
                scenarioInfoButton.setEnabled(true);
                scenarioInfoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent scenarioInfoIntent = new Intent(ScenariosActivity.this, ScenarioInfoActivity.class);
                        startActivity(scenarioInfoIntent);
                    }
                });

                final ImageView downloadButton = (ImageView) scenarioItemConvertView.findViewById(R.id.ico_download);
                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(true);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(ScenariosActivity.this);
                        clarifyDialogBuilder.setMessage(Html.fromHtml("Please confirm if you have <br>" +
                                Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()) + " free space <br>and turned on Wi-Fi?"))
                                .setCancelable(false)
                                .setPositiveButton("yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                                RecognitionScenarioService.startDownloading(recognitionScenario);
                                            }
                                        })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
                        clarifyDialog.show();

                    }
                });
                final ImageView deleteButton = (ImageView) scenarioItemConvertView.findViewById(R.id.ico_Delete);
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setEnabled(true);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //are you sure to delete
                        AlertDialog.Builder clarifyDialogBuilder = new AlertDialog.Builder(ScenariosActivity.this);
                        clarifyDialogBuilder.setMessage(Html.fromHtml("Are you sure to delete <br> all scenario's downloaded files?"))
                                .setCancelable(false)
                                .setPositiveButton("yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                                RecognitionScenarioService.deleteScenarioFiles(recognitionScenario);
                                            }
                                        })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                        final AlertDialog clarifyDialog = clarifyDialogBuilder.create();
                        clarifyDialog.show();
                    }
                });

                recognitionScenario.setButtonUpdater(new RecognitionScenarioService.Updater() {
                    @Override
                    public void update(final RecognitionScenario recognitionScenario) {

                        ScenariosActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                deleteButton.setEnabled(false);
                                deleteButton.setVisibility(View.GONE);

                                if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
                                    downloadButton.setImageResource(R.drawable.ico_preloading);
                                    downloadButton.setEnabled(false);
                                    downloadButton.setVisibility(View.VISIBLE);

                                    scenarioItemConvertView.refreshDrawableState();
                                } if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADED)) {
                                    downloadButton.setEnabled(false);
                                    downloadButton.setVisibility(View.GONE);

                                    deleteButton.setEnabled(true);
                                    deleteButton.setVisibility(View.VISIBLE);
                                }
                            }
                        });

                    }
                });
                recognitionScenario.getButtonUpdater().update(recognitionScenario);



//
//                    final int selected = i;
//
//                    LinearLayout ll = new LinearLayout(this);
//                    ll.setOrientation(LinearLayout.HORIZONTAL);
//                    ll.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            AppConfigService.updateSelectedRecognitionScenario(selected);
//                            Intent infoIntent = new Intent(ScenariosActivity.this, MainActivity.class);
//                            startActivity(infoIntent);
//                        }
//                    });
//
//                    final TextView resultItemView = new TextView(this);
//                    resultItemView.setPadding(0, 20, 0, 20);
//                    Spanned spanned = Html.fromHtml("<font color='#ffffff'><b>" + recognitionScenario.getTitle() + "</b></font>" );
//                    resultItemView.setText(spanned);
//                    ll.addView(resultItemView);
//
//                    final TextView volumeTextView = new TextView(this);
//                    volumeTextView.setPadding(0, 20, 0, 20);
//                    Spanned volumeTextViewSpanned = Html.fromHtml("<font color='#64ffda'><b>" + recognitionScenario.getTotalFileSize() + "</b></font>");
//                    volumeTextView.setText(volumeTextViewSpanned);
//                    ll.addView(volumeTextView);
//
//
//                    recognitionScenario.setProgressUpdater(new RecognitionScenarioService.Updater() {
//                        @Override
//                        public void update(final RecognitionScenario recognitionScenario) {
//
//                            ScenariosActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
//                                        volumeTextView.setText(
//                                                Utils.bytesIntoHumanReadable(recognitionScenario.getDownloadedTotalFileSizeBytes()) +
//                                                        " of " +
//                                                        Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
//                                        scenarioItemConvertView.refreshDrawableState();
//                                    } else {
//                                        volumeTextView.setText(
//                                                Utils.bytesIntoHumanReadable(recognitionScenario.getTotalFileSizeBytes()));
//                                    }
//                                }
//                            });
//
//                            // todo change view volumeTextView.
//                        }
//                    });
//                    recognitionScenario.getProgressUpdater().update(recognitionScenario);
//
//                    final ImageView downloadButton = new ImageView(this);
//                    downloadButton.setVisibility(View.VISIBLE);
//                    downloadButton.setEnabled(true);
//                    downloadButton.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            RecognitionScenarioService.startDownloading(recognitionScenario);
//                        }
//                    });
//                    ll.addView(downloadButton);
//                    recognitionScenario.setButtonUpdater(new RecognitionScenarioService.Updater() {
//                        @Override
//                        public void update(final RecognitionScenario recognitionScenario) {
//
//                            ScenariosActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADING_IN_PROGRESS)) {
//                                        downloadButton.setImageResource(R.drawable.ico_preloading);
//                                        downloadButton.setEnabled(false);
//                                        downloadButton.setVisibility(View.VISIBLE);
//                                        scenarioItemConvertView.refreshDrawableState();
//                                    } if (recognitionScenario.getState().equals(RecognitionScenario.State.DOWNLOADED)) {
//                                        downloadButton.setEnabled(false);
//                                        downloadButton.setVisibility(View.GONE);
//                                    }
//                                }
//                            });
//
//                        }
//                    });
//                    recognitionScenario.getButtonUpdater().update(recognitionScenario);
//
//                    resultRadioGroup.addView(ll);
            }
        }
    }
}