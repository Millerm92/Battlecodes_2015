package team302;

import battlecode.common.*;

import java.lang.System;
import java.util.*;

public class RobotPlayer {

    public static Team myTeam;
    public static Team enemyTeam;
    public static Random rand;
    public static MapLocation enemyHq;
    public static MapLocation myHq;
    public static MapLocation[] enemyTowers;
    public static MapLocation[] myTowers;
    public static MapLocation pointOfInterest;
    public static boolean weaponReady;
    public static boolean coreReady;
    public static int sensorRange;
    public static int attackRange;
    public static RobotType type;
    public static MapLocation myLocation;
    public static int myChannel;
    public static double attackPower;
    public static int channel;
    public static int hqBuildOffset;
    public static RobotInfo[] enemyRobots;
    public static Direction[] allDirections = Direction.values();
    public static Direction[] fastDirections = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST,  Direction.SOUTH_EAST,  Direction.SOUTH_WEST,  Direction.NORTH_WEST};
    public static int[] targets = MyConstants.assaultPriorities;
    public static int numInitialTowers;
    public static MapLocation[] initialEnemyTowerLocations;

	public static void run(RobotController rc) {
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        rand = new Random(rc.getID());
        enemyHq = rc.senseEnemyHQLocation();

        myHq = rc.senseHQLocation();
        myTowers = rc.senseTowerLocations();
		numInitialTowers = myTowers.length;
        pointOfInterest = rc.senseHQLocation();
		RobotType rt = rc.getType();
		sensorRange = rc.getType().sensorRadiusSquared;
		attackRange = rc.getType().attackRadiusSquared;
        type = rc.getType();
		attackPower = rc.getType().attackPower;
		hqBuildOffset = (RobotPlayer.myHq.x + RobotPlayer.myHq.y) % 2;

		while(true) {
            enemyTowers = rc.senseEnemyTowerLocations();
            enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
            myLocation = rc.getLocation();

			switch (rt) {
				case AEROSPACELAB:
					try {
						AEROSPACELAB.execute(rc);
					} catch (Exception e) {
						//System.out.println("AEROSPACELAB Exception");
						e.printStackTrace();
					}
					break;
				case BARRACKS:
					try {
						BARRACKS.execute(rc);
					} catch (Exception e) {
						//System.out.println("BARRACKS Exception");
						e.printStackTrace();
					}
					break;
				case BASHER:
					try {
						BASHER.execute(rc);
					} catch (Exception e) {
						//System.out.println("BASHER Exception");
						e.printStackTrace();
					}
					break;
				case BEAVER:
					try {
						BEAVER.execute(rc);
					} catch (Exception e) {
						//System.out.println("BEAVER Exception");
						e.printStackTrace();
					}
					break;
				case COMMANDER:
					try {
						COMMANDER.execute(rc);
					} catch (Exception e) {
						//System.out.println("COMMANDER Exception");
						e.printStackTrace();
					}
					break;
				case COMPUTER:
					try {
						COMPUTER.execute(rc);
					} catch (Exception e) {
						//System.out.println("COMPUTER Exception");
						e.printStackTrace();
					}
					break;
				case DRONE:
					try {
						DRONE.execute(rc);
					} catch (Exception e) {
						//System.out.println("DRONE Exception");
						e.printStackTrace();
					}
					break;
				case HANDWASHSTATION:
					try {
						HANDWASHSTATION.execute(rc);
					} catch (Exception e) {
						//System.out.println("HANDWASHSTATION Exception");
						e.printStackTrace();
					}
					break;
				case HELIPAD:
					try {
						HELIPAD.execute(rc);
					} catch (Exception e) {
						//System.out.println("HELIPAD Exception");
						e.printStackTrace();
					}
					break;
				case HQ:
					try {
						HQ.execute(rc);
					} catch (Exception e) {
						//System.out.println("HQ Exception");
						e.printStackTrace();
					}
					break;
				case LAUNCHER:
					try {
						LAUNCHER.execute(rc);
					} catch (Exception e) {
						//System.out.println("LAUNCHER Exception");
						e.printStackTrace();
					}
					break;
				case MINER:
					try {
						MINER.execute(rc);
					} catch (Exception e) {
						//System.out.println("MINER Exception");
						e.printStackTrace();
					}
					break;
				case MINERFACTORY:
					try {
						MINERFACTORY.execute(rc);
					} catch (Exception e) {
						//System.out.println("MINERFACTORY Exception");
						e.printStackTrace();
					}
					break;
				case MISSILE:
					try {
						MISSILE.execute(rc);
					} catch (Exception e) {
						//System.out.println("MISSILE Exception");
						e.printStackTrace();
					}
					break;
				case SOLDIER:
					try {
						SOLDIER.execute(rc);
					} catch (Exception e) {
						//System.out.println("SOLDIER Exception");
						e.printStackTrace();
					}
					break;
				case SUPPLYDEPOT:
					try {
						SUPPLYDEPOT.execute(rc);
					} catch (Exception e) {
						//System.out.println("SUPPLYDEPOT Exception");
						e.printStackTrace();
					}
					break;
				case TANK:
					try {
						TANK.execute(rc);
					} catch (Exception e) {
						//System.out.println("TANK Exception");
						e.printStackTrace();
					}
					break;
				case TANKFACTORY:
					try {
						TANKFACTORY.execute(rc);
					} catch (Exception e) {
						//System.out.println("TANKFACTORY Exception");
						e.printStackTrace();
					}
					break;
				case TECHNOLOGYINSTITUTE:
					try {
						TECHNOLOGYINSTITUTE.execute(rc);
					} catch (Exception e) {
						//System.out.println("TECHNOLOGYINSTITUTE Exception");
						e.printStackTrace();
					}
					break;
				case TOWER:
					try {
						TOWER.execute(rc);
					} catch (Exception e) {
						//System.out.println("TOWER Exception");
						e.printStackTrace();
					}
					break;
				case TRAININGFIELD:
					try {
						TRAININGFIELD.execute(rc);
					} catch (Exception e) {
						//System.out.println("TRAININGFIELD Exception");
						e.printStackTrace();
					}
					break;
			}
			rc.yield();
		}
	}
}
