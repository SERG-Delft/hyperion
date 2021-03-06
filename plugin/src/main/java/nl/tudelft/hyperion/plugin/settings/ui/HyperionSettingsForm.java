package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import nl.tudelft.hyperion.plugin.settings.HyperionSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that represents the visual layout of the settings page.
 * Saving the settings is handled by {@link HyperionSettings}.
 * <p>
 * Some fields are unused in this class but are necessary in order to bind and display
 * them through HyperionSettingsForm.form.
 */
@SuppressWarnings("unused")
public class HyperionSettingsForm {

    IntervalTable intervalTable;
    JTextField addressField;
    JTextField projectField;
    /**
     * UI Components.
     */
    private JPanel root;
    private IntervalListPanel intervalPanel;
    private JLabel addressTitle;
    private JLabel intervalsTitle;
    private JLabel projectLabel;
    /**
     * Other data needed.
     */
    private Project project;
    private HyperionSettings hyperionSettings;
    private boolean headless;

    /**
     * Instantiate Settings for given Project.
     *
     * @param project relates to the settings we need to load. {@see HyperionSettings#getInstance(Project)}
     */
    public HyperionSettingsForm(Project project) {
        this.project = project;
    }

    /**
     * Instantiate Settings for given Project.
     *
     * @param project  relates to the settings we need to load. {@see HyperionSettings#getInstance(Project)}
     * @param headless tells the Form whether it should run headless, which means it doesn't initialize itself but
     *                 it needs to be initialized manually.
     */
    public HyperionSettingsForm(Project project, boolean headless) {
        this.headless = headless;
        this.project = project;
    }

    public JPanel getRoot() {
        return root;
    }

    void createSettings() {
        hyperionSettings = HyperionSettings.Companion.getInstance(project);
    }

    void createTable() {
        List<Row> data = getIntervalRows();
        intervalTable = new IntervalTable(data);
    }

    void createPanel() {
        intervalPanel = new IntervalListPanel();
    }

    /**
     * Initialize any custom created UI Components here.
     * In our case this is only intervalTable and intervalPanel.
     */
    private void createUIComponents() {
        if (headless) return;
        createSettings();
        createTable();
        createPanel();
    }

    /**
     * Obtains the intervals (in seconds) from the state {@link HyperionSettings#getState()} and converts
     * them to rows {@link Row} for use in the IntervalTable.
     *
     * @return a List of rows {@link Row}
     */
    @NotNull
    List<Row> getIntervalRows() {
        List<Integer> intervals = hyperionSettings.getState().getIntervals();
        List<Row> data = new ArrayList<>();
        for (int interval : intervals) {
            data.add(Row.parse(interval));
        }
        return data;
    }

    /**
     * Asks the editable fields in the settings form if they have been modified or not.
     * This returns true only if the values differ compared to the last time {@link #apply()} has been called.
     *
     * @return true if any editable values were changed compared to last {@link #apply()} call.
     */
    public boolean isModified() {
        return intervalTable.isModified()
                || !addressField.getText().equals(hyperionSettings.getState().address)
                || !projectField.getText().equals(hyperionSettings.getState().project);
    }

    /**
     * Applies (saves) settings.
     * It converts values from {@link IntervalTable} to seconds {@link Row#toSeconds()}, saves the
     * API address and the Project name.
     */
    public void apply() {
        hyperionSettings.setAddress(addressField.getText());
        hyperionSettings.setProject(projectField.getText());

        List<Row> data = intervalTable.updateData();
        // Intervals in seconds
        List<Integer> intervals = new ArrayList<>();

        for (Row row : data) {
            int seconds = row.toSeconds();
            intervals.add(seconds);
        }

        hyperionSettings.setIntervals(intervals);
    }

    /**
     * Resets all editable fields to the previously applied values.
     * This can only be called when {@link #isModified()} returns true
     * (when false the reset button is invisible).
     */
    public void reset() {
        addressField.setText(hyperionSettings.getState().address);
        projectField.setText(hyperionSettings.getState().project);

        intervalTable.reset();
    }

    /**
     * Class that holds the {@link IntervalTable} and manages the add & delete buttons.
     */
    class IntervalListPanel extends JPanel {

        IntervalListPanel() {
            initPanel();
        }

        protected void initPanel() {
            final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(intervalTable)
                    .disableUpAction()
                    .disableDownAction()
                    .setAddAction(button -> addRow())
                    .setRemoveAction(button -> removeSelectedRow());
            setLayout(new BorderLayout());
            add(decorator.createPanel(), BorderLayout.CENTER);
        }

        private void removeSelectedRow() {
            // Convert the array to list and sort in reverse order.
            List<Integer> selectedRows = Arrays.stream(intervalTable.getSelectedRows())
                    .boxed().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (int row : selectedRows) {
                intervalTable.getIntervalTableModel().removeRow(row);
            }
        }

        private void addRow() {
            intervalTable.getIntervalTableModel().addRow(new Row(1, Period.SECONDS));
        }
    }
}
