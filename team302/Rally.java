package team302;
import battlecode.common.*;

import java.lang.Package;

public class Rally {
	public static RobotController rc;
	public static MapLocation myLocation;
	public static boolean defendDistressedTowers;
	public static MapLocation goal;
	public static RobotType goalType;// = RobotType.TOWER;
	public static RobotInfo[] enemyRobots;
	public static Direction[] allDirections = Direction.values();
	public static enum RallyState {
		UNASSIGNED,
		MOVING_TO_RALLY_LOC,
		RALLYING,
		SWARMING_GOAL,
		COLLAPSING_GOAL,
		DEFEND_TOWER
	};
	public static RallyState state = RallyState.UNASSIGNED;

	//broadcast offsets
	public static int RALLY_POINT;
	public static int SEND_TROOPS;
	public static int ATTACK_LOCATION;
	public static int TEMP_ATTACK_LOCATION;
	public static int ATTACK_LOCATION_TYPE;
	public static int SWARM_COUNT_POWER;
	public static int SWARM_COUNT_HEALTH;
	public static int SHOULD_COLLAPSE;

	/*
	 * decides what to do based on the robots current rally state and calls it
	 * also decides when to switch states
	 */
	public static void Rally(RobotController rc_in, int GROUP_OFFSET, boolean defendDistressedTowers_in) throws GameActionException {
		rc = rc_in;
		defendDistressedTowers = defendDistressedTowers_in;
		RALLY_POINT = GROUP_OFFSET;
		ATTACK_LOCATION = GROUP_OFFSET + 2;
		TEMP_ATTACK_LOCATION = GROUP_OFFSET + 4;
		SEND_TROOPS = GROUP_OFFSET + 6;
		ATTACK_LOCATION_TYPE = GROUP_OFFSET + 7;
		SWARM_COUNT_POWER = GROUP_OFFSET + 8;
		SWARM_COUNT_HEALTH = GROUP_OFFSET + 9;
		SHOULD_COLLAPSE = GROUP_OFFSET + 10;


		myLocation = RobotPlayer.myLocation;
		enemyRobots = rc.senseNearbyRobots(24, RobotPlayer.enemyTeam);
		goalType = RobotType.values()[rc.readBroadcast(ATTACK_LOCATION_TYPE)];
		makeMove();
	}

	private static void makeMove() throws GameActionException {
		int x, y;
		switch (state) {
		case UNASSIGNED:
			//get the rally point
			if (rc.getSupplyLevel() > 0) {
				state = RallyState.MOVING_TO_RALLY_LOC;
				makeMove();
			} else {
				Util.debug(rc, "MOVE TO SUPPLY");
				moveToSupply();
			}
			break;
		case MOVING_TO_RALLY_LOC:
			if (checkForDistressedTower()) {
				state = RallyState.DEFEND_TOWER;
				makeMove();
				return;
			}

			//get rally goal
			x = rc.readBroadcast(RALLY_POINT);
			y = rc.readBroadcast(RALLY_POINT + 1);
			goal = new MapLocation(x, y);
			Util.debug(rc, "MOVE TO RALLY POINT" + goal.toString());
			//if we are within 4 units^2 to goal we are rallying
			if (myLocation.distanceSquaredTo(goal) <= 6) {
				state = RallyState.RALLYING;
				makeMove();
			}else {
				Util.debug(rc, "MOVE TO RALLY");
				movingToRally();
			}
			break;
		case RALLYING:
			if (checkForDistressedTower()) {
				state = RallyState.DEFEND_TOWER;
				makeMove();
				return;
			}

			//get rally goal
			x = rc.readBroadcast(RALLY_POINT);
			y = rc.readBroadcast(RALLY_POINT + 1);
			goal = new MapLocation(x, y);

			//if our rally tower sends a send troops signal we are now swarming
			if (rc.readBroadcast(SEND_TROOPS) == 1) {
				x = rc.readBroadcast(ATTACK_LOCATION);
				y = rc.readBroadcast(ATTACK_LOCATION + 1);
				goal = new MapLocation(x, y);

				state = RallyState.SWARMING_GOAL;
				makeMove();
			} else {
				Util.debug(rc, "RALLYING");
				rallying();
			}
			break;
		case SWARMING_GOAL:
			if (checkForDistressedTower()) {
				state = RallyState.DEFEND_TOWER;
				makeMove();
				return;
			}

			// if there is a temp attack location go there
			x = rc.readBroadcast(TEMP_ATTACK_LOCATION);
			y = rc.readBroadcast(TEMP_ATTACK_LOCATION + 1);
			if (x != 0 && y != 0) {
				goal = new MapLocation(x,y);
				goalType = RobotType.values()[rc.readBroadcast(ATTACK_LOCATION_TYPE)];
			} else {
				x = rc.readBroadcast(ATTACK_LOCATION);
				y = rc.readBroadcast(ATTACK_LOCATION + 1);
				goalType = RobotType.values()[rc.readBroadcast(ATTACK_LOCATION_TYPE)];
				goal = new MapLocation(x, y);
			}

			if (readyToCollapse()) {
				state = RallyState.COLLAPSING_GOAL;
				collapsing();
			} else {
				Util.debug(rc, "SWARMIN");
				swarming();
				updateSwarmCount();
			}
			break;
		case COLLAPSING_GOAL:
			Util.debug(rc, "COLLAPSING");
			if (!collapsing()) {
				state = RallyState.SWARMING_GOAL;
				swarming();
				updateSwarmCount();
			}
			break;
		case DEFEND_TOWER:
			if (checkForDistressedTower()) {
				protectTower();
			} else {
				state = RallyState.MOVING_TO_RALLY_LOC;
				makeMove();
			}
		}

	}

	private static void updateSwarmCount() throws GameActionException {
		if (rc.getLocation().distanceSquaredTo(goal) <= goalType.attackRadiusSquared + 2) {
			rc.broadcast(SWARM_COUNT_POWER, (int) (rc.readBroadcast(SWARM_COUNT_POWER) + (RobotPlayer.type.attackPower / RobotPlayer.type.attackDelay)));
			rc.broadcast(SWARM_COUNT_HEALTH, (int) (rc.readBroadcast(SWARM_COUNT_HEALTH) + rc.getHealth()));
		}
	}

	/*
	 * Moves to hq for supply
	 */
	private static void moveToSupply() throws GameActionException {
		if (rc.isCoreReady()) {
			Direction dir = Bug.startBuggin(rc, RobotPlayer.myHq, 0);

			if (dir != Direction.OMNI && rc.canMove(dir) && !shouldFlee(myLocation.add(dir), false)) {
				rc.move(dir);
				MapLocation targetLocation = RobotPlayer.myLocation.add(dir);
				if (RobotPlayer.type == RobotType.TANK) {
					Util.requestSupply(rc, 10000, targetLocation);
				} else {
					Util.requestSupply(rc, 5000, targetLocation);
				}
			}
		}
	}

	/*
	 * if we should flee do so
	 * otherwise move to the rally spot
	 */
	private static void movingToRally() throws GameActionException {
		if (rc.isCoreReady()) {
			if (shouldFlee(myLocation, false)) {
				flee();
				return;
			}
			//if its
			Direction dir = Bug.startBuggin(rc, goal, 0);
			if (dir != Direction.OMNI && rc.canMove(dir) && !shouldFlee(myLocation.add(dir), false)) {
				rc.move(dir);
			}
		}
	}

	/*
	 * if we can attack someone do it
	 * otherwise rally around the rally spot
	 */
	private static void rallying() throws GameActionException {
		if (rc.isCoreReady()) {
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0){
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null){
					if (rc.canAttackLocation(target)){
						rc.attackLocation(target);
					}
				}
			} else {
				Direction dir = Bug.startBuggin(rc, goal, 0);
				if (dir != Direction.OMNI && rc.canMove(dir) && !shouldFlee(myLocation.add(dir), false)) {
					rc.move(dir);
				}

			}
		}
	}

	/*
	 * if we should flee do so, broadcast the confronted enemies location if we have not temp attack loc
	 * otherwise move towards the goal
	 */
	private static void swarming() throws GameActionException {
		if (rc.isCoreReady()) {
			//if i can sense the goal and no one is there set to temp attack loc to none
			updateGoal();

			//if we have no temp goal and we see an enemy we should broadcast it
			boolean tempGoalExists = false;
			int x = rc.readBroadcast(TEMP_ATTACK_LOCATION);
			int y = rc.readBroadcast(TEMP_ATTACK_LOCATION + 1);
			if (x == 0 && y == 0) tempGoalExists = true;

			if (shouldFlee(myLocation, tempGoalExists)) {
				flee();
				return;
			}
			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, goal, goalType.attackRadiusSquared);
			MapLocation newLoc = myLocation.add(dir);
			if (dir != Direction.OMNI && rc.canMove(dir) && !shouldFlee(newLoc, false)) {
				rc.move(dir);
				return;
			}
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0){
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null){
					if (rc.canAttackLocation(target)){
						rc.attackLocation(target);
					}
				}
			}
		}
	}

	/*
	 * if i can attack the goal do so
	 * otherwise move in towards the goal
	 *
	 * returns false if the enemy has been destroyed
	 */
	private static boolean collapsing() throws GameActionException {
		if (rc.isCoreReady()) {
			if (!updateGoal()) {
				if (rc.canAttackLocation(goal)) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(goal);
					}
				} else {
					Util.tryMove(rc, myLocation.directionTo(goal));
				}
				return true;
			} else {
				RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
				if (enemiesInRange.length > 0){
					MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
					if (target != null){
						if (rc.canAttackLocation(target)){
							rc.attackLocation(target);
						}
					}
				}
			}
		}

		return true;
	}

	/*
	 * swarms to tower and kills anyone it can within the towers attackradius
	 */
	private static void protectTower() throws GameActionException{
		if (rc.isCoreReady()) {
			if (myLocation.distanceSquaredTo(goal) < RobotType.TOWER.attackRadiusSquared + RobotPlayer.type.attackRadiusSquared) {

				RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
				if (enemiesInRange.length > 0){
					MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
					if (target != null){
						if (rc.canAttackLocation(target)){
							rc.attackLocation(target);
						}
					}
				} else {
					Util.tryMove(rc, myLocation.directionTo(goal));
				}
				return;
			}
			if (shouldFlee(myLocation, false)) {
				flee();
				return;
			}
			//move to the goal but don't go in its attack radius
			Direction dir = Bug.startBuggin(rc, goal, goalType.attackRadiusSquared);
			MapLocation newLoc = myLocation.add(dir);
			if (dir != Direction.OMNI && rc.canMove(dir) && !shouldFlee(newLoc, false)) {
				rc.move(dir);
				return;
			}
			RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
			if (enemiesInRange.length > 0){
				MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
				if (target != null){
					if (rc.canAttackLocation(target)){
						rc.attackLocation(target);
					}
				}
			}
		}
	}

	//UTILITY FUNCS

	/*
	 * returns true if someone can hit me at my current location
	 *
	 * broadcast - should i broadcast the enemies location as a temporary goal
	 */
	private static boolean shouldFlee(MapLocation loc, boolean broadcast) throws GameActionException {
		for (RobotInfo robot: enemyRobots) {
			if (loc.distanceSquaredTo(robot.location.add(robot.location.directionTo(loc))) <= robot.type.attackRadiusSquared) {
				if (broadcast) {
					int x = rc.readBroadcast(TEMP_ATTACK_LOCATION);
					int y = rc.readBroadcast(TEMP_ATTACK_LOCATION + 1);
					if (x == 0 && y == 0) {
						rc.broadcast(TEMP_ATTACK_LOCATION, robot.location.x);
						rc.broadcast(TEMP_ATTACK_LOCATION + 1, robot.location.y);
						rc.broadcast(ATTACK_LOCATION_TYPE, robot.type.ordinal());
					}
				}
				return true;
			}
		}

		//towers too
		for (MapLocation tloc: RobotPlayer.enemyTowers) {
			if (loc.distanceSquaredTo(tloc) <= RobotType.TOWER.attackRadiusSquared) {
				return true;
			}
		}

		if (loc.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
			return true;
		}

		return false;
	}

	/*
	 * Finds a location where no enemies can attack
	 *
	 *
	 * if no such location exists we just battle it out
	 */
	private static void flee() throws GameActionException {
		MapLocation desiredLoc;

		dirLoop:
			for (Direction d: allDirections) {
				if (d.equals(Direction.OMNI) || d.equals(Direction.NONE)) {
					continue;
				}
				desiredLoc = myLocation.add(d);

				//if i can move in the given direction
				if (rc.canMove(d)) {

					//iterate through each enemy bot
					for (RobotInfo robot : enemyRobots) {
						//could he hit me if he moved in
						if (desiredLoc.distanceSquaredTo(robot.location.add(robot.location.directionTo(desiredLoc))) <= robot.type.attackRadiusSquared) {
							// could he out chase me if i fled to where he cant hit me now?
							if (robot.type.movementDelay < RobotPlayer.type.movementDelay || desiredLoc.distanceSquaredTo(robot.location) <= robot.type.attackRadiusSquared) {
								continue dirLoop;
							}
						}
					}
					//can tower hit me
					for (MapLocation tloc: RobotPlayer.enemyTowers) {
						if (desiredLoc.distanceSquaredTo(tloc) <= RobotType.TOWER.attackRadiusSquared) {
							continue dirLoop;
						}
					}
					//can hq hit me
					if (desiredLoc.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
						continue dirLoop;
					}
					rc.move(d);
					return;
				}
			}

		RobotInfo[] enemiesInRange = rc.senseNearbyRobots(RobotPlayer.attackRange, RobotPlayer.enemyTeam);
		if (enemiesInRange.length > 0){
			MapLocation target = Util.smartAttack(rc, RobotPlayer.targets, enemiesInRange);
			if (target != null){
				if (rc.canAttackLocation(target)){
					rc.attackLocation(target);
				}
			}
		}
	}

	private static boolean readyToCollapse() throws GameActionException {
		if (rc.readBroadcast(SHOULD_COLLAPSE) == 1) {
			//System.out.println("GOOOOOOO");
			return true;
		}
		return false;
	}

	private static boolean checkForDistressedTower() throws GameActionException {
		if (defendDistressedTowers) {
			return false;
		}
		for (int i = 0; i < RobotPlayer.myTowers.length; i++) {
			if (rc.readBroadcast(MyConstants.TOWER_UNDER_DISTRESS + i) == 1) {
				goal = RobotPlayer.myTowers[i];
				return true;
			}
		}
		return false;
	}

	private static boolean updateGoal() throws GameActionException {
		if (rc.canSenseLocation(goal)) {
			RobotInfo botAtGoal = rc.senseRobotAtLocation(goal);
			if (botAtGoal != null && botAtGoal.team == RobotPlayer.enemyTeam) {
				return false;
			}

			for (Direction d: allDirections) {
				if (d.equals(Direction.OMNI) || d.equals(Direction.NONE)) {
					continue;
				}
				if (rc.canSenseLocation(goal.add(d))) {
					botAtGoal = rc.senseRobotAtLocation(goal.add(d));
					if (botAtGoal != null && botAtGoal.team == RobotPlayer.enemyTeam) {
						goal = botAtGoal.location;
						rc.broadcast(TEMP_ATTACK_LOCATION, goal.x);
						rc.broadcast(TEMP_ATTACK_LOCATION + 1, goal.y);
						rc.broadcast(ATTACK_LOCATION_TYPE, botAtGoal.type.ordinal());
						return false;
					}
				}
			}

			rc.broadcast(TEMP_ATTACK_LOCATION, 0);
			rc.broadcast(TEMP_ATTACK_LOCATION + 1, 0);

			int x = rc.readBroadcast(ATTACK_LOCATION);
			int y = rc.readBroadcast(ATTACK_LOCATION + 1);
			goal = new MapLocation(x, y);
			return true;
		}

		return false;
	}
}