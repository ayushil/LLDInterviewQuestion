import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FantasyLeaderboard {

    public static void main(String[] args) throws Exception {
        LeaderboardService service = new LeaderboardService(
                new InMemoryTeamRepository(),
                new StandardFantasyPointsCalculator(),
                new PointsThenNameRankingStrategy());

        service.registerTeam("T1", "Alpha XI");
        service.registerTeam("T2", "Beta Blasters");
        service.registerTeam("T3", "Gamma Giants");
        service.registerTeam("T4", "Delta Dynamos");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        pool.submit(() -> service.recordPerformance("T1", new TeamPerformance(2, 1, 0, 0)));
        Thread.sleep(300);
        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }
        pool.submit(() -> service.recordPerformance("T2", new TeamPerformance(1, 2, 1, 0)));
        Thread.sleep(300);
        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }
        pool.submit(() -> service.recordPerformance("T3", new TeamPerformance(3, 0, 0, 1)));
        Thread.sleep(300);
        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }
        pool.submit(() -> service.recordPerformance("T4", new TeamPerformance(0, 2, 1, 0)));
        Thread.sleep(300);
        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }
        pool.submit(() -> service.recordPerformance("T1", new TeamPerformance(1, 0, 1, 0)));
        Thread.sleep(300);
        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }
        pool.submit(() -> service.recordPerformance("T2", new TeamPerformance(0, 1, 1, 1)));
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("=== Leaderboard ===");
        List<TeamStanding> standings = service.allStandingsSorted();
        for (int i = 0; i < standings.size(); i++) {
            TeamStanding s = standings.get(i);
            System.out.println((i + 1) + ". " + s.getTeamName()
                    + " | points=" + s.getTotalPoints()
                    + " | weeks=" + s.getWeeksPlayed());
        }

        System.out.println("\n=== Top 2 ===");
        for (TeamStanding s : service.topN(2)) {
            System.out.println(s.getTeamName() + " -> " + s.getTotalPoints());
        }

        System.out.println("\nRank of T2: "
                + service.rankOf("T2").map(String::valueOf).orElse("not found"));
    }
}

final class TeamPerformance {
    private final int goals;
    private final int assists;
    private final int cleanSheets;
    private final int penaltiesSaved;

    TeamPerformance(int goals, int assists, int cleanSheets, int penaltiesSaved) {
        this.goals = goals;
        this.assists = assists;
        this.cleanSheets = cleanSheets;
        this.penaltiesSaved = penaltiesSaved;
    }

    int getGoals() { return goals; }
    int getAssists() { return assists; }
    int getCleanSheets() { return cleanSheets; }
    int getPenaltiesSaved() { return penaltiesSaved; }
}

interface FantasyPointsCalculator {
    int calculatePoints(TeamPerformance performance);
}

final class StandardFantasyPointsCalculator implements FantasyPointsCalculator {
    @Override
    public int calculatePoints(TeamPerformance performance) {
        return (performance.getGoals() * 5)
                + (performance.getAssists() * 3)
                + (performance.getCleanSheets() * 4)
                - (performance.getPenaltiesSaved());
    }
}

final class FantasyTeam {
    private final String teamId;
    private final String name;
    private final AtomicInteger totalPoints = new AtomicInteger(0);
    private final AtomicInteger gameWeeksPlayed = new AtomicInteger(0);

    FantasyTeam(String teamId, String name) {
        if (teamId == null || teamId.isEmpty()) throw new IllegalArgumentException("teamId");
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name");
        this.teamId = teamId;
        this.name = name;
    }

    String getTeamId() { return teamId; }
    String getName() { return name; }

    void addPoints(int pointsDelta) {
        totalPoints.addAndGet(pointsDelta);
        gameWeeksPlayed.incrementAndGet();
    }

    TeamStanding toStanding() {
        return new TeamStanding(teamId, name, totalPoints.get(), gameWeeksPlayed.get());
    }
}

final class TeamStanding {
    private final String teamId;
    private final String teamName;
    private final int totalPoints;
    private final int weeksPlayed;

    TeamStanding(String teamId, String teamName, int totalPoints, int weeksPlayed) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.totalPoints = totalPoints;
        this.weeksPlayed = weeksPlayed;
    }

    String getTeamId() { return teamId; }
    String getTeamName() { return teamName; }
    int getTotalPoints() { return totalPoints; }
    int getWeeksPlayed() { return weeksPlayed; }
}

interface TeamRepository {
    boolean add(FantasyTeam team);
    Optional<FantasyTeam> findById(String teamId);
    List<FantasyTeam> findAll();
}

final class InMemoryTeamRepository implements TeamRepository {
    private final ConcurrentHashMap<String, FantasyTeam> teams = new ConcurrentHashMap<>();

    @Override
    public boolean add(FantasyTeam team) {
        return teams.putIfAbsent(team.getTeamId(), team) == null;
    }

    @Override
    public Optional<FantasyTeam> findById(String teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }

    @Override
    public List<FantasyTeam> findAll() {
        return new ArrayList<>(teams.values());
    }
}

interface RankingStrategy {
    Comparator<TeamStanding> comparator();
}

final class PointsThenNameRankingStrategy implements RankingStrategy {
    @Override
    public Comparator<TeamStanding> comparator() {
        return Comparator.comparingInt(TeamStanding::getTotalPoints)
                .reversed()
                .thenComparing(TeamStanding::getTeamName);
    }
}

final class LeaderboardService {
    private final TeamRepository repository;
    private final FantasyPointsCalculator pointsCalculator;
    private final RankingStrategy rankingStrategy;

    LeaderboardService(TeamRepository repository,
                       FantasyPointsCalculator pointsCalculator,
                       RankingStrategy rankingStrategy) {
        this.repository = repository;
        this.pointsCalculator = pointsCalculator;
        this.rankingStrategy = rankingStrategy;
    }

    boolean registerTeam(String teamId, String teamName) {
        return repository.add(new FantasyTeam(teamId, teamName));
    }

    void recordPerformance(String teamId, TeamPerformance performance) {
        FantasyTeam team = repository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown team: " + teamId));
        int points = pointsCalculator.calculatePoints(performance);
        team.addPoints(points);
    }

    List<TeamStanding> topN(int n) {
        if (n <= 0) return List.of();
        List<TeamStanding> all = allStandingsSorted();
        return new ArrayList<>(all.subList(0, Math.min(n, all.size())));
    }

    Optional<Integer> rankOf(String teamId) {
        List<TeamStanding> all = allStandingsSorted();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getTeamId().equals(teamId)) return Optional.of(i + 1);
        }
        return Optional.empty();
    }

    List<TeamStanding> allStandingsSorted() {
        List<TeamStanding> standings = new ArrayList<>();
        for (FantasyTeam team : repository.findAll()) {
            standings.add(team.toStanding());
        }
        standings.sort(rankingStrategy.comparator());
        return standings;
    }
}