package org.opendatanode.plugins.extractor.sk.judge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.util.ObjectProperty;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;

@SuppressWarnings("serial")
public class SkJudgesToCsvVaadinDialog extends AbstractDialog<SkJudgesToCsvConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(SkJudgesToCsvVaadinDialog.class);

    private static final float COMPONENT_WIDTH_PERCENTAGE = 75;

    final private ObjectProperty<String> fileName = new ObjectProperty<String>("");


    public SkJudgesToCsvVaadinDialog() {
        super(SkJudgesToCsv.class);
    }

    @Override
    protected void buildDialogLayout() {

        setSizeFull();
        final VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setHeight("-1px");
        mainLayout.setMargin(true);

        mainLayout.setWidth(100, Unit.PERCENTAGE);


        TextField fileNameTextField = new TextField(ctx.tr("SkJudgesToCsvVaadinDialog.filename.label"), fileName);
        fileNameTextField.setInputPrompt("my_file.csv");
        fileNameTextField.setWidth(COMPONENT_WIDTH_PERCENTAGE, Unit.PERCENTAGE);

        mainLayout.addComponent(fileNameTextField);
        setCompositionRoot(mainLayout);
    }

    @Override
    protected SkJudgesToCsvConfig_V1 getConfiguration() throws DPUConfigException {
        SkJudgesToCsvConfig_V1 result = new SkJudgesToCsvConfig_V1();
        result.setFileName("");
        return result;
    }

    @Override
    protected void setConfiguration(SkJudgesToCsvConfig_V1 config) throws DPUConfigException {
        fileName.setValue(config.getFileName() == null ? "" : config.getFileName());
    }


}
