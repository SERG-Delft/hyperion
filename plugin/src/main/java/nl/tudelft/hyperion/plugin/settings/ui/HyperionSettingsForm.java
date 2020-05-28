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

public class HyperionSettingsForm {

    private JPanel root;
    private IntervalTable intervalTable;
    private IntervalListPanel intervalPanel;
    private JTextField addressField;
    private JLabel addressTitle;
    private JLabel intervalsTitle;

    private Project project;
    private HyperionSettings hyperionSettings;

    public HyperionSettingsForm(Project project) {
        super();
        this.project = project;
    }

    public JPanel getRoot() {
        return root;
    }

    /**
     * Initialize any custom created UI Components here.
     * In our case this is only intervalTable and intervalPanel.
     */
    private void createUIComponents() {
        hyperionSettings = HyperionSettings.Companion.getInstance(project);

        // Make sure we found an instance given the project.
        if (hyperionSettings == null) return;
        List<Row> data = getIntervalRows();
        intervalTable = new IntervalTable(data);
        intervalPanel = new IntervalListPanel();
    }

    @NotNull
    private List<Row> getIntervalRows() {
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
     * @return true if any editable values were changed compared to last {@link #apply()} call.
     */
    public boolean isModified() {
        return intervalTable.isModified() || !addressField.getText().equals(hyperionSettings.getState().address);
    }

    public void apply() {
        hyperionSettings.setAddress(addressField.getText());

        List<Row> data = intervalTable.updateData();
        // Intervals in seconds
        List<Integer> intervals = new ArrayList<>();

        for (Row row : data) {
            int seconds = row.toSeconds();
            intervals.add(seconds);
        }

        hyperionSettings.setIntervals(intervals);
    }

    public void reset() {
        addressField.setText(hyperionSettings.getState().address);

        intervalTable.reset();
    }

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
            intervalTable.getIntervalTableModel().addRow(new Row(1, Period.Seconds));
        }

    }
}
