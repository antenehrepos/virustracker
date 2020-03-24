package et.bots.thevirustrackerbot.stats;

import et.bots.thevirustrackerbot.StatsDTO;
import et.bots.thevirustrackerbot.event.v1.StatsUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class DailyStatsServiceImpl implements DailyStatsService {

    private static final long TIMEOUT_VALUE=60;
    private static final TimeUnit TIMEOUT_UNIT=TimeUnit.SECONDS;

    private final DailyStatsRepository dailyStatsRepository;
    private final ProducerTemplate statsProducerTemplate;
    private final ProducerTemplate eventProducerTemplate;

    @Override
    public Iterable<DailyStats> findAll() {
        return dailyStatsRepository.findAll();
    }

    @Override
   public Optional<DailyStats> findByCountryCodeProvinceAndDateRange(String countryCode, String province, LocalDateTime from, LocalDateTime to) {
        return dailyStatsRepository.findByCountryCodeAndProvinceAndDateBetween(countryCode,province,from, to);
    }

    @Override
    public DailyStats create(DailyStats dailyStats) {
        return dailyStatsRepository.save(dailyStats);
    }

    @Override
    public DailyStats update(DailyStats dailyStats) {
        return dailyStatsRepository.save(dailyStats);
    }

    @Override
    public void delete(DailyStats dailyStats) {
        dailyStatsRepository.delete(dailyStats);
    }

    @Override
    public void updateStats(String countryCode, String subscriberId){
        try {
            List<StatsDTO> statsDTOs = statsProducerTemplate.asyncRequestBodyAndHeader(StatsConstants.ENDPOINT_GET_STATS,"", StatsConstants.HEADER_COUNTRY_CODE, countryCode, List.class).get(TIMEOUT_VALUE, TIMEOUT_UNIT);
            statsDTOs
                    .forEach(statsDTO -> {

                        Optional<DailyStats> existing = this.findByCountryCodeProvinceAndDateRange(statsDTO.getCountryCode(), statsDTO.getProvince(), LocalDate.now().atStartOfDay(), LocalDateTime.now());

                        if(!existing.isPresent()) {
                            this.create(DailyStats
                                    .builder()
                                    .countryCode(statsDTO.getCountryCode())
                                    .province(statsDTO.getProvince())
                                    .latitude(statsDTO.getLatitude())
                                    .longitude(statsDTO.getLongitude())
                                    .date(LocalDateTime.now())
                                    .totalCases(statsDTO.getTotalCases())
                                    .totalDeaths(statsDTO.getTotalDeaths())
                                    .totalRecovered(statsDTO.getTotalRecovered())
                                    .newCases(0)
                                    .newDeaths(0)
                                    .newRecoveries(0)
                                    .build());

                            eventProducerTemplate.sendBody(StatsUpdatedEvent.builder().statsDTO(statsDTO).build());
                        }else if(statsDTO.getTotalCases() > existing.get().getTotalCases()
                                || statsDTO.getTotalDeaths() > existing.get().getTotalDeaths()
                                || statsDTO.getTotalRecovered() > existing.get().getTotalRecovered()){

                            DailyStats dailyStats = existing.get();
                            dailyStats.setNewCases(dailyStats.getNewCases()+ (statsDTO.getTotalCases() - dailyStats.getTotalCases()));
                            dailyStats.setNewDeaths(dailyStats.getNewDeaths()+ (statsDTO.getTotalDeaths() - dailyStats.getTotalDeaths()));
                            dailyStats.setNewRecoveries(dailyStats.getNewRecoveries()+ (statsDTO.getTotalRecovered() - dailyStats.getTotalRecovered()));

                            dailyStats.setTotalCases(statsDTO.getTotalCases());
                            dailyStats.setTotalDeaths(statsDTO.getTotalDeaths());
                            dailyStats.setTotalRecovered(statsDTO.getTotalRecovered());

                            this.update(dailyStats);
                            eventProducerTemplate.sendBody(StatsUpdatedEvent.builder().statsDTO(statsDTO).build());
                        }else if(!StringUtils.isEmpty(subscriberId)){

                            DailyStats dailyStats = existing.get();

                            statsDTO.setNewCases(dailyStats.getNewCases());
                            statsDTO.setNewDeaths(dailyStats.getNewDeaths());
                            statsDTO.setNewRecoveries(dailyStats.getNewRecoveries());

                            statsDTO.setTotalCases(dailyStats.getTotalCases());
                            statsDTO.setTotalDeaths(dailyStats.getTotalDeaths());
                            statsDTO.setTotalRecovered(dailyStats.getTotalRecovered());

                            eventProducerTemplate.sendBody(StatsUpdatedEvent.builder().subscriberId(subscriberId).statsDTO(statsDTO).build());
                        }
                    });
        } catch (Exception e) {
            log.error("Unable to update status: {}" ,e.getMessage(), e);
        }
    }
}