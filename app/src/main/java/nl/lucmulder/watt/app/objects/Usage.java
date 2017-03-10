package nl.lucmulder.watt.app.objects;

/**
 * Created by lcm on 6-3-2017.
 */

public class Usage {
    public String huidig;
    public String piek;
    public String dal;
    public String gas;
    public String timestamp;
    public String min;
    public String max;
    public String minToday;
    public String maxToday;
    public SingleUsage sinceMorning;
    public SingleUsageExtended[] lastMinutes;
    public SingleUsageExtended[] lastHour;
    public SingleUsage[] lastDay;
    public SingleUsage[] lastWeek;
    public SingleUsage[] lastMonth;
    public SingleUsage[] lastYear;
    public First first;
    public SingleUsage thisWeek;
    public SingleUsage thisMonth;
}
