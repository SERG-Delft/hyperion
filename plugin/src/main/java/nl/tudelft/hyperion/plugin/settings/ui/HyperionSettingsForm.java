package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import nl.tudelft.hyperion.plugin.settings.HyperionSettings;

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
        hyperionSettings = HyperionSettings.Companion.getInstance(project);
    }

    public JPanel getRoot() {
        return root;
    }

    private void createUIComponents() {

        List<Integer> intervals = hyperionSettings.getState().getIntervals();
        List<Row> data = new ArrayList<>();
        for (int interval : intervals) {

            data.add(Row.parse(interval));
        }
        intervalTable = new IntervalTable(data);
        intervalPanel = new IntervalListPanel();
    }

    public boolean isModified() {
        return intervalTable.isModified();
    }

    public void apply() {
        List<Row> data = intervalTable.updateData();
        // Intervals in seconds
        List<Integer> intervals = new ArrayList<>();

        for (Row row : data) {
            int seconds = row.toSeconds();
            intervals.add(seconds);
        }

        hyperionSettings.setIntervals(intervals);
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
