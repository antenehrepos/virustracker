
package et.bots.thevirustrackerbot.stats.integration.tvt.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CountryData {

    @JsonProperty("countrydata")
    private List<Countrydatum> countrydata = null;
    @JsonProperty("stat")
    private String stat;

}
