package juliushenke.smarttt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private int selected_day_of_week = 0;
    private int saved_change = 0;

    private int selected_hour = 1;
    private String selected_subject = "";
    private int selectedColorRGB = 0;

    private boolean selected_evenWeeks = true;
    private boolean week_isOdd = false;

    private boolean editing_mode = false;
    private boolean days_changed = false;

    private boolean selected_activity_main = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivity_main();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.act_editMode:
                changeMode();
                return true;

            case R.id.act_settings:
                setActivity_options();
                return true;

            case R.id.act_deleteSub:
                Dialog select_subject = select_subject(true);
                select_subject.show();
                return true;

            case R.id.act_help:
                Dialog tutorial = help();
                tutorial.show();
                return true;

            case R.id.act_aboutApp:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(null).setMessage(getResources().getString(R.string.aboutApp));
                builder.show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (selected_activity_main) {
                if (editing_mode) {
                    changeMode();
                } else finish();
            } else {
                setActivity_main();
                if (days_changed) {
                    days_changed = false;
                    saved_change = 0;
                    setActivity_main();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //Voids --------------------------------------------------------------------------------
    private void setActivity_main() {
        if (!selected_activity_main) setContentView(R.layout.activity_main);
        selected_activity_main = true;
        setDay(0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
    }

    private void setActivity_options() {
        setContentView(R.layout.activity_options);
        selected_activity_main = false;
        update_activityOptions();

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
    }

    private void setActivity_subject(boolean newSubject) {
        setContentView(R.layout.activity_subject);
        selected_activity_main = false;

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        EditText TF_subject = (EditText) findViewById(R.id.TF_subject);
        EditText TF_room = (EditText) findViewById(R.id.TF_room);
        EditText TF_teacher = (EditText) findViewById(R.id.TF_teacher);
        Button B_subject_color = (Button) findViewById(R.id.B_subject_color);

        if (newSubject) {
            TF_subject.setText("");
            TF_room.setText("");
            TF_teacher.setText("");

            selectedColorRGB = R.color.color_B_hour;
            B_subject_color.getBackground().setColorFilter(null);
            B_subject_color.setTextColor(Color.BLACK);
        } else {
            try {
                String filename = "SUBJECT-" + selected_subject + ".srl";
                ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                Subject subject = (Subject) input.readObject();

                TF_subject.setText(subject.getName());
                TF_room.setText(subject.getRoom());
                TF_teacher.setText(subject.getTeacher());

                selectedColorRGB = subject.getColor();
                B_subject_color.getBackground().setColorFilter(subject.getColor(), PorterDuff.Mode.MULTIPLY);
                if (isColorDark(selectedColorRGB)) B_subject_color.setTextColor(Color.WHITE);
                else B_subject_color.setTextColor(Color.BLACK);

                input.close();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setDay(int input_change) {
        Resources r = getResources();

        //Reading settings
        String filename = "SETTINGS.srl";
        Settings settings = new Settings();
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
            settings = (Settings) input.readObject();
        } catch (FileNotFoundException e) {
            try {
                ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                out.writeObject(settings);
                out.close();

                editing_mode = true;
                input_change = 0;
                Dialog tutorial = help();
                tutorial.show();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            e.printStackTrace();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        Calendar c = Calendar.getInstance();
        saved_change = saved_change + input_change;
        c.add(Calendar.DAY_OF_YEAR, saved_change);
        selected_day_of_week = getDay_of_week(c);

        int extra_change = 0;
        //positive change - into the future
        if (input_change >= 0) {
            if (selected_day_of_week < settings.getStart_day())
                extra_change = settings.getStart_day() - selected_day_of_week;
            else if (selected_day_of_week > settings.getEnd_day())
                extra_change = 7 - selected_day_of_week + settings.getStart_day();
        }
        //negative change - into the past
        else if (input_change < 0) {
            if (selected_day_of_week < settings.getStart_day())
                extra_change = -selected_day_of_week - (7 - settings.getEnd_day());
            else if (selected_day_of_week > settings.getEnd_day())
                extra_change = settings.getEnd_day() - selected_day_of_week;
        }
        //update the Calendar
        c.add(Calendar.DAY_OF_YEAR, extra_change);
        saved_change = saved_change + extra_change;

        //update the day_of_week
        selected_day_of_week = getDay_of_week(c);

        String[] days = r.getStringArray(R.array.days);
        TextView TV_main_day = (TextView) findViewById(R.id.TV_main_day);
        TV_main_day.setText(days[selected_day_of_week]);

        Button B_main_dateCenter = (Button) findViewById(R.id.B_main_dateCenter);
        Button B_main_weekType = (Button) findViewById(R.id.B_main_weekType);
        Button B_main_copyWeek = (Button) findViewById(R.id.B_main_copyWeek);

        int date_week = c.get(Calendar.WEEK_OF_YEAR);
        week_isOdd = isOdd(date_week);

        if (editing_mode) {
            B_main_dateCenter.setText(R.string.finish_edit);
            B_main_dateCenter.setTextColor(Color.BLUE);
            if (settings.getWeekSystem()) {
                B_main_weekType.setVisibility(View.VISIBLE);
                B_main_copyWeek.setVisibility(View.VISIBLE);
                if (selected_evenWeeks) {
                    B_main_weekType.setText(R.string.evenWeek);
                    B_main_copyWeek.setText(R.string.copyOddWeek);
                } else {
                    B_main_weekType.setText(R.string.oddWeek);
                    B_main_copyWeek.setText(R.string.copyEvenWeek);
                }
            } else {
                B_main_weekType.setVisibility(View.GONE);
                B_main_copyWeek.setVisibility(View.GONE);
            }
        } else {
            DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            String string_date = DATE_FORMAT.format(c.getTime());
            if (settings.getShowWeek())
                B_main_dateCenter.setText(string_date + " (" + r.getString(R.string.week) + " " + date_week + ")");
            else B_main_dateCenter.setText(string_date);
            B_main_dateCenter.setTextColor(Color.BLACK);
            B_main_weekType.setVisibility(View.GONE);
            B_main_copyWeek.setVisibility(View.GONE);
        }

        final TextView[] TVs_room = {null,
                (TextView) findViewById(R.id.r1), (TextView) findViewById(R.id.r2), (TextView) findViewById(R.id.r3),
                (TextView) findViewById(R.id.r4), (TextView) findViewById(R.id.r5), (TextView) findViewById(R.id.r6),
                (TextView) findViewById(R.id.r7), (TextView) findViewById(R.id.r8), (TextView) findViewById(R.id.r9),
                (TextView) findViewById(R.id.r10), (TextView) findViewById(R.id.r11)};

        final TextView[] TVs_hour = {null,
                (TextView) findViewById(R.id.n1), (TextView) findViewById(R.id.n2), (TextView) findViewById(R.id.n3),
                (TextView) findViewById(R.id.n4), (TextView) findViewById(R.id.n5), (TextView) findViewById(R.id.n6),
                (TextView) findViewById(R.id.n7), (TextView) findViewById(R.id.n8), (TextView) findViewById(R.id.n9),
                (TextView) findViewById(R.id.n10), (TextView) findViewById(R.id.n11)};

        final Button[] Bts = {null,
                (Button) findViewById(R.id.h1), (Button) findViewById(R.id.h2), (Button) findViewById(R.id.h3),
                (Button) findViewById(R.id.h4), (Button) findViewById(R.id.h5), (Button) findViewById(R.id.h6),
                (Button) findViewById(R.id.h7), (Button) findViewById(R.id.h8), (Button) findViewById(R.id.h9),
                (Button) findViewById(R.id.h10), (Button) findViewById(R.id.h11)};

        for (int i = 1; i <= 11; i++) {
            TVs_hour[i].setVisibility(View.INVISIBLE);
            Bts[i].setVisibility(View.INVISIBLE);
            TVs_room[i].setText("");
        }
        for (int i = 1; i <= 11; i++) {
            TVs_hour[i].setVisibility(View.VISIBLE);
            Bts[i].getBackground().setColorFilter(null);
            Bts[i].setTextColor(Color.BLACK);
            Bts[i].getBackground().setColorFilter(null);
            Bts[i].setTextColor(Color.BLACK);
            Bts[i].setText("+");
            if (editing_mode) Bts[i].setVisibility(View.VISIBLE);
        }
        try {
            filename = getFilenameForDay();
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
            String[] hours = (String[]) input.readObject();
            for (int i = 1; i <= 11; i++) {
                try {
                    filename = "SUBJECT-" + hours[i] + ".srl";
                    input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                    Subject s = (Subject) input.readObject();

                    Bts[i].setVisibility(View.VISIBLE);
                    if (!s.getName().equals(hours[i - 1])) TVs_room[i].setText(s.getRoom());
                    Bts[i].setText(s.getName());
                    Bts[i].getBackground().setColorFilter(s.getColor(), PorterDuff.Mode.MULTIPLY);
                    if (isColorDark(s.getColor())) Bts[i].setTextColor(Color.WHITE);
                } catch (FileNotFoundException e) {
                    filename = getFilenameForDay();
                    try {
                        hours[i] = "";
                        ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                        out.writeObject(hours);
                        out.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    e.printStackTrace();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
            input.close();
        } catch (FileNotFoundException e) {
            filename = getFilenameForDay();
            String[] hours = new String[12];

            if (filename.equals("ODD-DAY-" + selected_day_of_week + ".srl"))
                overwriteEvenDay(false);
            else {
                try {
                    ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                    out.writeObject(hours);
                    out.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                e.printStackTrace();
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void update_activityOptions() {
        String filename = "SETTINGS.srl";
        Settings settings = new Settings();
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
            settings = (Settings) input.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        String[] days = getResources().getStringArray(R.array.days);
        Button B_startDay = (Button) findViewById(R.id.B_opt_startDay);
        Button B_endDay = (Button) findViewById(R.id.B_opt_endDay);
        Switch Switch_opt_weekSystem = (Switch) findViewById(R.id.Switch_opt_weekSystem);
        Switch Switch_opt_showWeek = (Switch) findViewById(R.id.Switch_opt_showWeek);

        B_startDay.setText(days[settings.getStart_day()]);
        B_endDay.setText(days[settings.getEnd_day()]);
        if (settings.getWeekSystem()) Switch_opt_weekSystem.setChecked(true);
        else Switch_opt_weekSystem.setChecked(false);
        if (settings.getShowWeek()) Switch_opt_showWeek.setChecked(true);
        else Switch_opt_showWeek.setChecked(false);

        Switch_opt_weekSystem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String filename = "SETTINGS.srl";
                Settings settings = new Settings();
                try {
                    ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                    settings = (Settings) input.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }

                if (isChecked) {
                    settings.setWeekSystem(true);
                } else {
                    settings.setWeekSystem(false);
                }

                try {
                    ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                    out.writeObject(settings);
                    out.close();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Switch_opt_showWeek.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String filename = "SETTINGS.srl";
                Settings settings = new Settings();
                try {
                    ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                    settings = (Settings) input.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }

                if (isChecked) {
                    settings.setShowWeek(true);
                } else {
                    settings.setShowWeek(false);
                }

                try {
                    ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                    out.writeObject(settings);
                    out.close();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void changeMode() {
        if (!editing_mode) {
            editing_mode = true;
            setActivity_main();
        } else {
            editing_mode = false;
            saved_change = 0;
            setActivity_main();
        }
    }

    private void overwriteEvenDay(boolean overwrite_evenDay) {
        String[] hours = new String[12];

        String inputName = "DAY-" + selected_day_of_week + ".srl";
        String outputName = "ODD-DAY-" + selected_day_of_week + ".srl";
        if (overwrite_evenDay) {
            inputName = "ODD-DAY-" + selected_day_of_week + ".srl";
            outputName = "DAY-" + selected_day_of_week + ".srl";
        }

        //read the input
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + inputName)));
            hours = (String[]) input.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        //overwrite the output with input
        try {
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + outputName));
            out.writeObject(hours);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setActivity_main();
    }

    //Clickers ---------------------------------------------------------------------------------
    public void clickB_main_hour(View view) {
        Button button = (Button) view;
        selected_subject = button.getText().toString();

        if (view == findViewById(R.id.h1)) selected_hour = 1;
        else if (view == findViewById(R.id.h2)) selected_hour = 2;
        else if (view == findViewById(R.id.h3)) selected_hour = 3;
        else if (view == findViewById(R.id.h4)) selected_hour = 4;
        else if (view == findViewById(R.id.h5)) selected_hour = 5;
        else if (view == findViewById(R.id.h6)) selected_hour = 6;
        else if (view == findViewById(R.id.h7)) selected_hour = 7;
        else if (view == findViewById(R.id.h8)) selected_hour = 8;
        else if (view == findViewById(R.id.h9)) selected_hour = 9;
        else if (view == findViewById(R.id.h10)) selected_hour = 10;
        else if (view == findViewById(R.id.h11)) selected_hour = 11;

        if (!editing_mode) {
            setActivity_subject(false);
        } else {
            File[] files = getSubjectFiles();
            if (files.length > 0) {
                Resources r = getResources();
                if (selected_subject.equals("+") || selected_subject.equals("")) {
                    String[] options = {r.getString(R.string.D_select_subject), r.getString(R.string.D_createSubject)};
                    Dialog edit_hour = edit_hour(options);
                    edit_hour.show();
                } else {
                    String[] options = {r.getString(R.string.D_select_subject), r.getString(R.string.D_createSubject), r.getString(R.string.D_freeHour)};
                    Dialog edit_hour = edit_hour(options);
                    edit_hour.show();
                }
            } else {
                setActivity_subject(true);
            }
        }
    }

    public void clickB_opt_startDay(View view) {
        Dialog manage_days = manage_days(true);
        manage_days.show();
    }

    public void clickB_opt_endDay(View view) {
        Dialog manage_days = manage_days(false);
        manage_days.show();
    }

    public void clickB_sub_color(View view) {
        final ColorPicker cp = new ColorPicker(MainActivity.this, Color.red(selectedColorRGB), Color.green(selectedColorRGB), Color.blue(selectedColorRGB));
        cp.show();

    /* On Click listener for the dialog, when the user select the color */
        Button okColor = (Button) cp.findViewById(R.id.okColorButton);
        okColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* You can get single channel (value 0-255) */
                //selectedColorR = cp.getRed();
                //selectedColorG = cp.getGreen();
                //selectedColorB = cp.getBlue();

                /* Or the android RGB Color (see the android Color class reference) */
                selectedColorRGB = cp.getColor();

                Button B_subject_color = (Button) findViewById(R.id.B_subject_color);
                B_subject_color.getBackground().setColorFilter(selectedColorRGB, PorterDuff.Mode.MULTIPLY);
                if (isColorDark(selectedColorRGB)) B_subject_color.setTextColor(Color.WHITE);
                else B_subject_color.setTextColor(Color.BLACK);

                cp.dismiss();
            }
        });
    }

    public void clickB_sub_save(View view) {
        EditText TF_subject = (EditText) findViewById(R.id.TF_subject);
        EditText TF_room = (EditText) findViewById(R.id.TF_room);
        EditText TF_teacher = (EditText) findViewById(R.id.TF_teacher);

        String name = TF_subject.getText().toString().trim();
        String room = TF_room.getText().toString().trim();
        String teacher = TF_teacher.getText().toString().trim();

        if (!name.equals("") && !name.equals("+")) {
            Subject subject = new Subject(name, room, teacher, selectedColorRGB);

            try {
                String filename = "SUBJECT-" + subject.getName() + ".srl";
                ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                out.writeObject(subject);
                out.close();
                try {
                    filename = getFilenameForDay();
                    ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                    String[] hours = (String[]) input.readObject();
                    hours[selected_hour] = name;
                    if ((selected_hour == 1 || selected_hour == 3 || selected_hour == 5) && (hours[selected_hour + 1].equals("") || hours[selected_hour + 1] == null)) {
                        hours[selected_hour + 1] = name;
                    }
                    input.close();

                    out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                    out.writeObject(hours);
                    out.close();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

            setActivity_main();
        } else
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_need_name), Toast.LENGTH_LONG).show();
    }

    public void click_B_main_weekType(View view) {
        Button B_main_weekType = (Button) findViewById(R.id.B_main_weekType);

        PopupMenu popup = new PopupMenu(MainActivity.this, B_main_weekType);
        //Inflating the Popup using xml file
        if (selected_evenWeeks)
            popup.getMenuInflater().inflate(R.menu.menu_even_weeks, popup.getMenu());
        else popup.getMenuInflater().inflate(R.menu.menu_odd_weeks, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.act_evenWeeks) selected_evenWeeks = true;
                else if (item.getItemId() == R.id.act_oddWeeks) selected_evenWeeks = false;
                setActivity_main();
                return true;
            }
        });

        popup.show();
    }

    public void click_B_main_copyWeek(View view) {
        if (selected_evenWeeks) overwriteEvenDay(true);
        else overwriteEvenDay(false);
    }

    public void click_dateLeft(View view) {
        setDay(-1);
        setActivity_main();
    }

    public void click_dateCenter(View view) {
        Button b = (Button) findViewById(R.id.B_main_dateCenter);

        if (editing_mode) {
            changeMode();
            b.setTextColor(Color.BLACK);
        } else {
            saved_change = 0;
        }
        setActivity_main();
    }

    public void click_dateRight(View view) {
        setDay(+1);
        setActivity_main();
    }

    //Dialogs ---------------------------------------------------------------------------------
    private Dialog edit_hour(String[] options) {

        //Initialize the Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = getResources().getString(R.string.dialog2_title, selected_hour);

        builder.setTitle(title).setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Dialog select_subject = select_subject(false);
                    select_subject.show();
                } else if (which == 1) {
                    setActivity_subject(true);
                } else if (which == 2) {
                    try {
                        String filename = getFilenameForDay();
                        ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                        String[] hours = (String[]) input.readObject();
                        input.close();
                        hours[selected_hour] = null;

                        try {
                            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                            out.writeObject(hours);
                            out.close();
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                            setActivity_main();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
                dialog.cancel();
            }
        });
        return builder.create();
    }

    private Dialog select_subject(final boolean delete) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        File[] files = getSubjectFiles();
        final String[] subject_names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                String filename = files[i].getName();
                ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));

                Subject subject = (Subject) input.readObject();
                subject_names[i] = subject.getName();
                input.close();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }

        String title = getResources().getString(R.string.dialog3_title_1, selected_hour);
        if (delete) title = getResources().getString(R.string.D_delete_subject);
        builder.setTitle(title).setSingleChoiceItems(subject_names, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!delete) {
                    try {
                        String filename = getFilenameForDay();
                        ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                        String[] hours = (String[]) input.readObject();
                        hours[selected_hour] = subject_names[which];
                        if ((selected_hour == 1 || selected_hour == 3 || selected_hour == 5) && (hours[selected_hour + 1].equals("") || hours[selected_hour + 1] == null)) {
                            hours[selected_hour + 1] = subject_names[which];
                        }
                        input.close();
                        try {
                            ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                            out.writeObject(hours);
                            out.close();

                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        File dir = getFilesDir();
                        File file = new File(dir, "SUBJECT-" + subject_names[which] + ".srl");
                        boolean deleted = file.delete();
                        if (deleted)
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_deleted), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (selected_activity_main) setDay(0);
                dialog.cancel();
            }
        });
        return builder.create();
    }

    private Dialog manage_days(final boolean startDay) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] days = getResources().getStringArray(R.array.days);
        String[] options = {days[1], days[2], days[3], days[4], days[5], days[6], days[7]};

        builder.setTitle(null).setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings settings = new Settings();
                String filename = "SETTINGS.srl";
                try {
                    ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
                    settings = (Settings) input.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
                if (startDay) settings.setStart_day(which + 1);
                else settings.setEnd_day(which + 1);

                try {
                    ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                    out.writeObject(settings);
                    out.close();
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Toast_saved), Toast.LENGTH_SHORT).show();
                    days_changed = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                update_activityOptions();
                dialog.cancel();
            }
        });
        return builder.create();
    }

    private Dialog help() {
        Resources r = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(r.getString(R.string.D_title_help)).setMessage(r.getString(R.string.D_help));
        return builder.create();
    }

    //Checkers ---------------------------------------------------------------------------------
    boolean isOdd(int val) {
        return (val & 0x01) != 0;
    }

    public boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    //Getters ---------------------------------------------------------------------------------
    private int getDay_of_week(Calendar c) {
        int day_of_week = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (day_of_week == -1) day_of_week = 6;
        else if (day_of_week == 0) day_of_week = 7;
        return day_of_week;
    }

    private File[] getSubjectFiles() {
        return getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("SUBJECT-");
            }
        });
    }

    private String getFilenameForDay() {
        String filename = "SETTINGS.srl";
        Settings settings = new Settings();
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + filename)));
            settings = (Settings) input.readObject();
        } catch (FileNotFoundException e) {
            try {
                ObjectOutput out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + filename));
                out.writeObject(settings);
                out.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            e.printStackTrace();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        if (settings.getWeekSystem()) {
            if (editing_mode && !selected_evenWeeks)
                return "ODD-DAY-" + selected_day_of_week + ".srl";
            else if (!editing_mode && week_isOdd) return "ODD-DAY-" + selected_day_of_week + ".srl";
        }
        return "DAY-" + selected_day_of_week + ".srl";
    }
}


