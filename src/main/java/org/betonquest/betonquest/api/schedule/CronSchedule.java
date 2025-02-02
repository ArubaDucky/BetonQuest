package org.betonquest.betonquest.api.schedule;

import com.cronutils.model.Cron;
import com.cronutils.model.SingleCron;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.modules.schedule.ScheduleID;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A schedule using <a href="https://crontab.guru/">cron syntax</a> for defining time instructions.
 */
public abstract class CronSchedule extends Schedule {

    /**
     * The unix cron syntax shall be used by default.
     */
    public static final CronDefinition DEFAULT_CRON_DEFINITION = CronDefinitionBuilder.defineCron()
            .withMinutes().withValidRange(0, 59).withStrictRange().and()
            .withHours().withValidRange(0, 23).withStrictRange().and()
            .withDayOfMonth().withValidRange(1, 31).withStrictRange().and()
            .withMonth().withValidRange(1, 12).withStrictRange().and()
            .withDayOfWeek().withValidRange(0, 7).withMondayDoWValue(1).withIntMapping(7, 0).withStrictRange().and()
            .withSupportedNicknameYearly().withSupportedNicknameAnnually()
            .withSupportedNicknameMonthly().withSupportedNicknameWeekly()
            .withSupportedNicknameMidnight().withSupportedNicknameDaily()
            .withSupportedNicknameHourly()
            .instance();

    /**
     * Statement that means that the schedule should run on reboot.
     */
    private static final String REBOOT_ALIAS = "@reboot";

    /**
     * Cron expression that defines when the events from this schedule shall run.
     */
    protected final Cron timeCron;

    /**
     * If events from this schedule shall run on reboot.
     */
    protected final boolean onReboot;

    /**
     * Provides information when the events from this schedule shall be executed.
     */
    protected final ExecutionTime executionTime;

    /**
     * Creates new instance of the schedule.
     * It should parse all options from the configuration section.
     * If anything goes wrong, throw {@link InstructionParseException} with an error message describing the problem.
     *
     * @param scheduleId  id of the new schedule
     * @param instruction config defining the schedule
     * @throws InstructionParseException if parsing the config failed
     */
    public CronSchedule(final ScheduleID scheduleId, final ConfigurationSection instruction) throws InstructionParseException {
        this(scheduleId, instruction, DEFAULT_CRON_DEFINITION, false);
    }

    /**
     * Alternative constructor that provides a way to use a custom cron syntax for this schedule.
     * <b>Make sure to create a constructor with the following two arguments when extending this class:</b>
     * {@code ScheduleID id, ConfigurationSection instruction}
     *
     * @param scheduleID      id of the new schedule
     * @param instruction     config defining the schedule
     * @param cronDefinition  a custom cron syntax, you may use {@link #DEFAULT_CRON_DEFINITION}
     * @param rebootSupported if {@code  @reboot} statement is supported by this schedule type
     * @throws InstructionParseException if parsing the config failed
     */
    protected CronSchedule(final ScheduleID scheduleID, final ConfigurationSection instruction,
                           final CronDefinition cronDefinition, final boolean rebootSupported) throws InstructionParseException {
        super(scheduleID, instruction);
        if (rebootSupported && REBOOT_ALIAS.equals(super.time.trim().toLowerCase(Locale.ROOT))) {
            this.timeCron = new SingleCron(cronDefinition, List.of());
            this.onReboot = true;
            this.executionTime = new NoExecutionTime();
        } else {
            try {
                this.timeCron = new CronParser(cronDefinition).parse(super.time).validate();
                this.onReboot = false;
                this.executionTime = ExecutionTime.forCron(timeCron);
            } catch (final IllegalArgumentException e) {
                throw new InstructionParseException("Time is no valid cron syntax: '" + super.time + "'", e);
            }
        }
    }

    /**
     * Get the cron expression that defines when the events from this schedule shall run.
     *
     * @return cron expression parsed from {@link #getTime()} string
     */
    public Cron getTimeCron() {
        return timeCron;
    }

    /**
     * Check if the schedule should run on reboot.
     * If {@code  @reboot} statement is not supported by this schedule type this method will return false.
     *
     * @return true if schedule should run on reboot, false otherwise
     */
    public boolean shouldRunOnReboot() {
        return onReboot;
    }

    /**
     * Get information when the events from this schedule shall be executed.
     *
     * @return execution time helper as defined by cron-utils
     */
    public ExecutionTime getExecutionTime() {
        return executionTime;
    }

    /**
     * Utility method that simplifies getting the next execution time of this schedule as instant.
     *
     * @return Optional containing the next time of execution as instant. Empty if there is no next execution.
     */
    public Optional<Instant> getNextExecution() {
        return executionTime.nextExecution(ZonedDateTime.now()).map(Instant::from);
    }

    /**
     * Utility method that simplifies getting the last execution time of this schedule as instant.
     *
     * @return Optional containing the last time of execution as instant. Empty if there is no last execution time.
     */
    public Optional<Instant> getLastExecution() {
        return executionTime.lastExecution(ZonedDateTime.now()).map(Instant::from);
    }

    /**
     * Implementation for {@link ExecutionTime} that cannot calculate any execution times.
     * <p>
     * Used for {@code @reboot} schedules
     */
    private static class NoExecutionTime implements ExecutionTime {

        @Override
        public Optional<ZonedDateTime> nextExecution(final ZonedDateTime date) {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> timeToNextExecution(final ZonedDateTime date) {
            return Optional.empty();
        }

        @Override
        public Optional<ZonedDateTime> lastExecution(final ZonedDateTime date) {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> timeFromLastExecution(final ZonedDateTime date) {
            return Optional.empty();
        }

        @Override
        public boolean isMatch(final ZonedDateTime date) {
            return false;
        }
    }
}
