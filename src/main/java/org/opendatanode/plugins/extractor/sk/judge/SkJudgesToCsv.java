package org.opendatanode.plugins.extractor.sk.judge;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.files.FilesDataUnit.Entry;
import eu.unifiedviews.dataunit.files.WritableFilesDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUContext.MessageType;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dataunit.files.FilesDataUnitUtils;
import eu.unifiedviews.helpers.dpu.config.ConfigHistory;
import eu.unifiedviews.helpers.dpu.context.ContextUtils;
import eu.unifiedviews.helpers.dpu.exec.AbstractDpu;

@DPU.AsExtractor
public class SkJudgesToCsv extends AbstractDpu<SkJudgesToCsvConfig_V1> {

    private static final Logger LOG = LoggerFactory.getLogger(SkJudgesToCsv.class);

    private static final String DEFAULT_CSV_NAME = "judges_sk.csv";

    private static final String URL_SK_JUDGES = "http://www.justice.gov.sk/Stranky/Sudcovia/SudcaZoznam.aspx";

    @DataUnit.AsOutput(name = "output")
    public WritableFilesDataUnit filesOutput;

    private DPUContext context;

    public SkJudgesToCsv() {
        super(SkJudgesToCsvVaadinDialog.class,
                ConfigHistory.noHistory(SkJudgesToCsvConfig_V1.class));
    }

    @Override
    protected void innerExecute() throws DPUException {
        this.context = this.ctx.getExecMasterContext().getDpuContext();
        String shortMessage = this.ctx.tr("dpu.sk.judges.starting", this.getClass().getSimpleName());
        String longMessage = String.valueOf(this.config);
        this.context.sendMessage(DPUContext.MessageType.INFO, shortMessage, longMessage);


        if (ctx.canceled()) {
            throw ContextUtils.dpuExceptionCancelled(ctx);
        }

        final String fileName = config.getFileName() == null || config.getFileName().isEmpty()
                ? DEFAULT_CSV_NAME
                : config.getFileName();

        try {
            LOG.debug("Creating output file in files data unit with name {}", fileName);
            Entry destinationFile = FilesDataUnitUtils.createFile(this.filesOutput, fileName);

            // TODO set metadata?
            
            final List<Judge> judges = new ArrayList<Judge>();
            JudgeSkCrawler.getJudgesPost(URL_SK_JUDGES, 1, judges, new ArrayList<NameValuePair>());
            
            final File csvFile = new File(URI.create(destinationFile.getFileURIString()));
            JudgeToCSV.toCSV(judges, csvFile);
            
        } catch (DataUnitException e) {
            ContextUtils.dpuException(this.ctx, e, "errors.destination.file");
        } catch (Exception e) {
            String errMsg = "Failed to harvest judges.";
            LOG.error(errMsg, e);
            this.context.sendMessage(MessageType.ERROR, errMsg, e.getMessage());
        }
    }
}
