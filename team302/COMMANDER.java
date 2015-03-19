package team302;

import battlecode.common.*;

import java.lang.System;

public class COMMANDER {
	public static final int FLASHDISTANCESQUARED = 10;
	public static RobotController rc;
	public static double hp;
	public static boolean firstSupplied = false;

	public static enum CommanderState {
		AGGRESSIVE,
		DEFENSIVE
	};

	public static CommanderState state = CommanderState.AGGRESSIVE;
	public static MapLocation pointOfInterest = RobotPlayer.enemyHq;
	public static boolean defendingHQ = false;

	public static void execute(RobotController rc_in) throws GameActionException {
		rc = rc_in;
		hp = rc.getHealth();
		RobotPlayer.targets = MyConstants.assaultPriorities;
		RobotPlayer.myChannel = MyConstants.COMMANDER_SUPPLY_REQUEST_OFFSET;
		rc.broadcast(RobotPlayer.myChannel, Clock.getRoundNum());

		if (!firstSupplied) {
			Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);
			if(rc.canMove(dir) && rc.isCoreReady()){
				rc.move(dir);
				Util.requestSupply(rc, 10000, rc.getLocation().add(dir));
			}
			if(rc.getSupplyLevel() > 9000) {
				firstSupplied = true;
			}
		}

		if (!defendingHQ && (Clock.getRoundNum() >= 1000 || rc.getXP() >= 1400)) {
			rc.setIndicatorString(0, "RALLYING");
			pointOfInterest = new MapLocation(rc.readBroadcast(MyConstants.MAIN_GOAL), rc.readBroadcast(MyConstants.MAIN_GOAL + 1));
		} else if (!defendingHQ) {
			rc.setIndicatorString(0, "RUSHING ENEMY HQ");
			pointOfInterest = RobotPlayer.enemyHq;
		}

		double totalDeeps = 0;
		for (RobotInfo enemyRobot: RobotPlayer.enemyRobots) {
			totalDeeps += enemyRobot.type.attackPower;
		}
		if (hp > 190 || (rc.getXP() >= 1500 && RobotPlayer.enemyRobots.length < 3)) {
			state = CommanderState.AGGRESSIVE;
		} else if (hp < totalDeeps) {
			state = CommanderState.DEFENSIVE;
		}

		switch (state) {
		case AGGRESSIVE:
			aggressiveMicro();
			break;
		case DEFENSIVE:
			safeMicro();
			break;
		default:
			System.out.println("WTF WHY ARE WE HERE, emmettz lickz BALZ SAKZ");
		}

		if (rc.getSupplyLevel() < 750) {
			rc.broadcast(RobotPlayer.myChannel + 1, 1);
		} else if (rc.getSupplyLevel() >= 3000) {
			rc.broadcast(RobotPlayer.myChannel + 1, 0);
		}

	}

	public static void aggressiveMicro() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
		
		int numContainingEnemies = rc.readBroadcast(MyConstants.NUM_CONTAINING_ENEMIES_OFFSET);
		int teamInRoute = rc.readBroadcast(MyConstants.TEAM_IN_ROUTE);
		
		if (!defendingHQ){
			if (numContainingEnemies > 0 && teamInRoute == 0){
				defendingHQ = true;
				pointOfInterest = Util.defendHQ(rc, numContainingEnemies);
				rc.broadcast(MyConstants.TEAM_IN_ROUTE, 1);
			}
		}

		if (defendingHQ){
			if (rc.getLocation().distanceSquaredTo(pointOfInterest) < 9){
				if (numContainingEnemies < 1){
					defendingHQ = false;
					pointOfInterest = RobotPlayer.enemyHq;
					rc.broadcast(MyConstants.TEAM_IN_ROUTE, 0);

				} else {
					pointOfInterest = Util.defendHQ(rc,  numContainingEnemies);
				}
			}
		}

		if (rc.isCoreReady()) {
			if (enemyRobots.length > 0) {
				RobotInfo closestEnemy = enemyRobots[0];
				int closestDistance = 9999;
				int enemyDistance = -1;
				for (RobotInfo robot : enemyRobots) {
					if (!Util.safeToMoveTo(rc, RobotPlayer.myLocation.add(RobotPlayer.myLocation.directionTo(robot.location)))) {
						continue;
					}
					enemyDistance = robot.location.distanceSquaredTo(RobotPlayer.myLocation);
					if (closestDistance > enemyDistance) {
						closestDistance = enemyDistance;
						closestEnemy = robot;
					}
				}

				Direction dir = RobotPlayer.myLocation.directionTo(closestEnemy.location);
				if (closestDistance > 10) {
					MapLocation newLoc = RobotPlayer.myLocation.add(dir);
					rc.broadcast(RobotPlayer.myChannel + 2, newLoc.x);
					rc.broadcast(RobotPlayer.myChannel + 3, newLoc.y);
					Util.moveToLocation(rc, newLoc);
				}
			} else {
				Direction d = Bug.startBuggin(rc, pointOfInterest, 0);
				Direction dirToDest = RobotPlayer.myLocation.directionTo(pointOfInterest);

				if (d != dirToDest) {
					if (!tryFlashInDirection(dirToDest)) {
						if (rc.canMove(d) && rc.isCoreReady()) {
							MapLocation newLoc = rc.getLocation().add(d);
							rc.broadcast(RobotPlayer.myChannel + 2, newLoc.x);
							rc.broadcast(RobotPlayer.myChannel + 3, newLoc.y);
							rc.move(d);
						}
					}
				} else {
					if (rc.canMove(d) && rc.isCoreReady()) {
						MapLocation newLoc = rc.getLocation().add(d);
						rc.broadcast(RobotPlayer.myChannel + 2, newLoc.x);
						rc.broadcast(RobotPlayer.myChannel + 3, newLoc.y);
						rc.move(d);
					}
				}
			}
		}

		RobotInfo[] enemyRobotsInAttackRange = rc.senseNearbyRobots(10, RobotPlayer.enemyTeam);

		if (rc.isWeaponReady()) {
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0) {
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null) {
					if (rc.canAttackLocation(target)) {
						rc.attackLocation(target);
						return;
					}
				}
			}
		}
	}

	public static void safeMicro() throws GameActionException {
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
		if (enemyRobots.length == 0) {
			return;
		}
		RobotInfo closestEnemy = enemyRobots[0];
		int closestDistance = 9999;
		int enemyDistance = -1;
		for (RobotInfo robot: enemyRobots) {
			enemyDistance = robot.location.distanceSquaredTo(RobotPlayer.myLocation);
			if (closestDistance > enemyDistance) {
				closestDistance = enemyDistance;
				closestEnemy = robot;
			}
		}

		if (closestEnemy.type.attackRadiusSquared > closestDistance) {
			if (closestDistance <= 10) {
				if (rc.isWeaponReady()) {
					rc.attackLocation(closestEnemy.location);
				}
			}
		}
		Direction dir = Util.fleeNew(rc);
		if (!tryFlashInDirection(dir) && rc.isCoreReady()) {
			Util.moveToLocation(rc, RobotPlayer.myLocation.add(dir));
		}
	}

	public static boolean tryFlashInDirection(Direction d) throws GameActionException {
		MapLocation targetLocation = RobotPlayer.myLocation;

		if (rc.getFlashCooldown() > 0) {
			return false;
		}

		// find furthest square away in the given distance
		while (RobotPlayer.myLocation.distanceSquaredTo(targetLocation.add(d)) <= FLASHDISTANCESQUARED) {
			targetLocation = targetLocation.add(d);
		}

		if (isOpenSafeSquare(targetLocation) && rc.isCoreReady()) {
			rc.broadcast(RobotPlayer.myChannel + 2, targetLocation.x);
			rc.broadcast(RobotPlayer.myChannel + 3, targetLocation.y);
			rc.castFlash(targetLocation);
			return true;
		}

		// try to flash to surrounding locations
		for (Direction dir: Util.directions) {
			MapLocation newLocation = targetLocation.add(dir);
			// filter out locations that are out of flash range
			if (RobotPlayer.myLocation.distanceSquaredTo(newLocation) > FLASHDISTANCESQUARED) {
				continue;
			}

			if (isOpenSafeSquare(newLocation) && rc.isCoreReady()) {
				rc.broadcast(RobotPlayer.myChannel + 2, targetLocation.x);
				rc.broadcast(RobotPlayer.myChannel + 3, targetLocation.y);
				rc.castFlash(newLocation);
				return true;
			}
		}
		return false;
	}

	public static boolean isOpenSafeSquare(MapLocation targetLocation) throws GameActionException {

		// dont consider squares within enemy tower range or hq range
		if (!Util.safeToMoveTo(rc, targetLocation)) {
			return false;
		}

		// dont move into a square ocupied by a robot
		if (rc.senseRobotAtLocation(targetLocation) != null) {
			return false;
		}

		// don't move onto a tile that is not traversable
		if (!rc.senseTerrainTile(targetLocation).isTraversable()) {
			return false;
		}

		return true;
	}
}