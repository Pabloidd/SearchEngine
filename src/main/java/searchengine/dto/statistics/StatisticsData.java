package searchengine.dto.statistics;

import lombok.Data;
import java.util.List;

/**
 * Данные статистики (общая + детальная)
 */
@Data
public class StatisticsData {
    private TotalStatistics total; // Общая статистика
    private List<DetailedStatisticsItem> detailed; // Детальная статистика по сайтам
}