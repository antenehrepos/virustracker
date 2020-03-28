package et.bots.thevirustrackerbot.stats.integration;

import et.bots.thevirustrackerbot.stats.integration.provider.StatsProvider;
import org.apache.camel.Processor;

public interface GetCountryStatsResponseTranslator extends Processor {
    
    boolean supports(StatsProvider provider);
}
