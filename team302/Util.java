package team302;

import battlecode.common.*;

import java.util.*;

public class Util {
	public static Random rand = new Random();
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};


	/*
	 * spawns the current spawn type if possible
	 */
	public static boolean buildWithPrecedence(RobotController rc, Direction d, RobotType[] canBuild) throws GameActionException{
		if (!rc.isCoreReady()) {
			return false;
		}

		int numToBuild;
		double myOre = rc.getTeamOre();

		for (RobotType type: canBuild) {
			numToBuild = rc.readBroadcast(MyConstants.SPAWN_TYPE_OFFSET + type.ordinal());
			if (numToBuild > 0 && myOre >= type.oreCost) {
				tryBuild(rc, d, type);
				numToBuild--;
				rc.broadcast(MyConstants.SPAWN_TYPE_OFFSET + type.ordinal(), numToBuild);
				return true;
			}
		}
		return false;
	}

	/*
	 * spawns the current spawn type if possible
	 */
	public static boolean spawnWithPrecedence(RobotController rc, Direction d, RobotType[] canSpawn) throws GameActionException{
		if (!rc.isCoreReady()) {
			return false;
		}

		int numToSpawn;
		double myOre = rc.getTeamOre();

		for (RobotType type: canSpawn) {
			numToSpawn = rc.readBroadcast(MyConstants.SPAWN_TYPE_OFFSET + type.ordinal());
			if (numToSpawn > 0 && myOre >= type.oreCost) {
				trySpawn(rc, d, type);
				numToSpawn--;
				rc.broadcast(MyConstants.SPAWN_TYPE_OFFSET + type.ordinal(), numToSpawn);
				return true;
			}
		}
		return false;
	}

	public static int mapLocToInt(MapLocation m)  throws GameActionException{
		return (m.x*10000 + m.y);
	}

	public static MapLocation intToMapLoc(int i)  throws GameActionException{
		return new MapLocation(i/10000,i%10000);
	}

	// This method will attempt to move in Direction d (or as close to it as possible)
	public static void tryMove(RobotController rc, Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && (!rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8]))) {
			offsetIndex++;
		}
		if (offsetIndex < 5 && rc.isCoreReady()) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}

	public static Direction tryMoveForMiners(RobotController rc, Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && (!rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])||
				!safeToMoveTo(rc, rc.getLocation().add(directions[(dirint+offsets[offsetIndex]+8)%8])))) {
			offsetIndex++;
		}
		if (offsetIndex < 5 && rc.isCoreReady()) {
			Direction dir = directions[(dirint+offsets[offsetIndex]+8)%8];
			return dir;
		}

		return Direction.NONE;
	}

	// This method will attack an enemy in sight, if there is one
	public static void attackSomething(RobotController rc, int myRange, Team enemyTeam) throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	public static MapLocation getNewPointOfInterest(RobotController rc) throws GameActionException{
		int numPointsOfInterest = rc.readBroadcast(MyConstants.NUM_POINTS_OF_INTEREST_OFFSET);
		int offSet = MyConstants.POINTS_OF_INTEREST_OFFSET;

		int random = RobotPlayer.rand.nextInt(numPointsOfInterest);
		random = 2 * random;
		MapLocation pointOfInterest = new MapLocation(rc.readBroadcast(offSet + random), rc.readBroadcast(offSet + random + 1));

		return pointOfInterest;

	}

	public static boolean safeToMoveTo(RobotController rc, MapLocation myLocation) throws GameActionException {

		if (RobotPlayer.enemyTowers.length >= 5) {
			if (myLocation.distanceSquaredTo(RobotPlayer.enemyHq) <= 49) {
				return false;
			}
		} else if (RobotPlayer.enemyTowers.length >= 2) {
			if (myLocation.distanceSquaredTo(RobotPlayer.enemyHq) <= 35) {
				return false;
			}
		} else {
			if (myLocation.distanceSquaredTo(RobotPlayer.enemyHq) <= 24) {
				return false;
			}
		}

		for (MapLocation tower: RobotPlayer.enemyTowers) {
			if (myLocation.distanceSquaredTo(tower) <= 24) {
				return false;
			}
		}
		return true;
	}

	// This function will try to move to a given location. It will move around enemy towers and HQ.
	public static void moveToLocation(RobotController rc, MapLocation goal) throws GameActionException {
		if (!rc.isCoreReady()) {
			return;
		}

		Direction goalDir = RobotPlayer.myLocation.directionTo(goal);
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(goalDir);

		for (int i = 0; i < offsets.length; i++) {
			Direction targetDir = directions[(dirint+offsets[i]+8)%8];

			if (!rc.canMove(targetDir)) {
				//continue in loop if we cant move to the target location
				continue;
			}

			MapLocation targetLocation = RobotPlayer.myLocation.add(targetDir);
			if (!safeToMoveTo(rc, targetLocation)) {
				continue;
			}

			// we can move to the target location if we have made it this far.
			rc.move(targetDir);
			debug(rc, "Moving to location: " + targetLocation);
			break;
		}
	}

	// This method will attempt to build in the given direction (or as close to it as possible)
	public static void tryBuild(RobotController rc, Direction d, RobotType type) throws GameActionException {

		for (Direction dir : Direction.values()) {
			if (dir == Direction.NONE || dir == Direction.OMNI) {
				continue;
			}
			MapLocation adj = RobotPlayer.myLocation.add(dir);
			if (((adj.x + adj.y) % 2) == RobotPlayer.hqBuildOffset && rc.canBuild(dir, type)) {
				rc.build(dir, type);
				return;
			}
		}

		tryMove(rc, intToDirection(RobotPlayer.rand.nextInt(8)));
		return;

	}


	//use this to return the map location in a specific direction from your robot
	public static MapLocation getAdjacentLocation(RobotController rc, Direction dir, MapLocation loc) throws GameActionException {
		return (loc.add(dir));
	}



	//prefer adjacent tiles with more ore
	//compare ore at current location to ore at all adjacent tiles
	//move to the most fruitful tile, unless there are several w/ same amount, then pick one of them at random
	//frig
	public static void SmartMine(RobotController rc, int myChannel) throws GameActionException {
		if (!rc.isCoreReady()) {
			return;
		}
	
		if(shouldFlee(rc, RobotPlayer.myLocation, false)){
			Direction f = Util.fleeNew(rc);
			if (f != Direction.NONE && f != Direction.OMNI && rc.canMove(f) && rc.isCoreReady()) {
				MapLocation added = RobotPlayer.myLocation.add(f);
				rc.broadcast(myChannel + 2, added.x);
				rc.broadcast(myChannel + 3, added.y);
				rc.move(f);
				return;
			}
			Util.attack(rc, RobotPlayer.enemyRobots);
			rc.broadcast(myChannel + 2, RobotPlayer.myLocation.x);
			rc.broadcast(myChannel + 3, RobotPlayer.myLocation.y);
			return;
		} else {
			MapLocation target = new MapLocation(rc.readBroadcast(MyConstants.MOST_ORE_LOCATION_X),
					rc.readBroadcast(MyConstants.MOST_ORE_LOCATION_Y));
			int numOre = (int) rc.senseOre(RobotPlayer.myLocation);
			if (Clock.getRoundNum() % 20 == 0 && rc.readBroadcast(MyConstants.MOST_ORE) < numOre) {
				rc.broadcast(MyConstants.MOST_ORE, numOre);
				rc.broadcast(MyConstants.MOST_ORE_LOCATION_X, RobotPlayer.myLocation.x);
				rc.broadcast(MyConstants.MOST_ORE_LOCATION_Y, RobotPlayer.myLocation.y);
				//System.out.println("UPDATING");
			}
			else if (Clock.getRoundNum() % 20 == 0 && rc.readBroadcast(MyConstants.MOST_ORE) == numOre 
					&& RobotPlayer.myLocation.distanceSquaredTo(target) > 16) {
				rc.broadcast(MyConstants.MOST_ORE, numOre);
				rc.broadcast(MyConstants.MOST_ORE_LOCATION_X, RobotPlayer.myLocation.x);
				rc.broadcast(MyConstants.MOST_ORE_LOCATION_Y, RobotPlayer.myLocation.y);
				//System.out.println("UPDATING");
			}
				
			MapLocation myLocation = rc.getLocation();
			double oreCount = rc.senseOre(myLocation);
			ArrayList<Integer> OreLocations = new ArrayList<Integer>();
	
			double ore_N = rc.senseOre(getAdjacentLocation(rc, Direction.NORTH, myLocation));
			double ore_NE = rc.senseOre(getAdjacentLocation(rc, Direction.NORTH_EAST, myLocation));
			double ore_E = rc.senseOre(getAdjacentLocation(rc, Direction.EAST, myLocation));
			double ore_SE = rc.senseOre(getAdjacentLocation(rc, Direction.SOUTH_EAST, myLocation));
			double ore_S = rc.senseOre(getAdjacentLocation(rc, Direction.SOUTH, myLocation));
			double ore_SW = rc.senseOre(getAdjacentLocation(rc, Direction.SOUTH_WEST, myLocation));
			double ore_W = rc.senseOre(getAdjacentLocation(rc, Direction.WEST, myLocation));
			double ore_NW = rc.senseOre(getAdjacentLocation(rc, Direction.NORTH_WEST, myLocation));

			int avgOre;

			if(RobotPlayer.myTowers.length > 0){
				avgOre = rc.readBroadcast(MyConstants.AVG_ORE) / RobotPlayer.myTowers.length;
			}else {
				avgOre = rc.readBroadcast(MyConstants.AVG_ORE);
			}
			int amt = avgOre / 4;
			if (amt < 2) {
				amt = 2;
			}
			if (amt * oreCount < ore_N && rc.canMove(Direction.NORTH) 
					&& ore_N > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.NORTH.ordinal());
			if (amt * oreCount < ore_NE && rc.canMove(Direction.NORTH_EAST) 
					&& ore_NE > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.NORTH_EAST.ordinal());
			if (amt * oreCount < ore_E && rc.canMove(Direction.EAST) 
					&& ore_E > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.EAST.ordinal());
			if (amt * oreCount < ore_SE && rc.canMove(Direction.SOUTH_EAST) 
					&& ore_SE > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.SOUTH_EAST.ordinal());
			if (amt * oreCount < ore_S && rc.canMove(Direction.SOUTH) 
					&& ore_S > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.SOUTH.ordinal());
			if (amt * oreCount < ore_SW && rc.canMove(Direction.SOUTH_WEST) 
					&& ore_SW > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.SOUTH_WEST.ordinal());
			if (amt * oreCount < ore_W && rc.canMove(Direction.WEST) 
					&& ore_W > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.WEST.ordinal());
			if (amt * oreCount < ore_NW && rc.canMove(Direction.NORTH_WEST) 
					&& ore_NW > rc.readBroadcast(MyConstants.MOST_ORE) / 9)
				OreLocations.add(Direction.NORTH_WEST.ordinal());
	
			//if there are no locations adjacent with more ore than current location
			if (OreLocations.size() == 0) {
				if (oreCount > rc.readBroadcast(MyConstants.MOST_ORE) / 9) {
					rc.broadcast(myChannel + 2, rc.getLocation().x);
					rc.broadcast(myChannel + 3, rc.getLocation().y);
					rc.mine();
					return;
				} else {
					if(RobotPlayer.myLocation.distanceSquaredTo(target) > 9){
						Direction dir = Bug.startBuggin(rc, target, 3);
						if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir)) {
							MapLocation newD = RobotPlayer.myLocation.add(dir);
							rc.broadcast(myChannel + 2, newD.x);
							rc.broadcast(myChannel + 3, newD.y);
							rc.move(dir);
							return;
						}
					} else {
						rc.broadcast(myChannel + 2, rc.getLocation().x);
						rc.broadcast(myChannel + 3, rc.getLocation().y);
						rc.mine();
						return;
					}
				}
			} else {
				int pick = RobotPlayer.rand.nextInt(OreLocations.size());
				Direction dir = Util.tryMoveForMiners(rc, intToDirection((int) OreLocations.get(pick)));
				if (dir != Direction.NONE && dir != Direction.OMNI && rc.canMove(dir)) {
					MapLocation newD = RobotPlayer.myLocation.add(dir);
					rc.broadcast(myChannel + 2, newD.x);
					rc.broadcast(myChannel + 3, newD.y);
					rc.move(dir);
					return;
				}
			}
		}
}


	public static Direction intToDirection(int i)  throws GameActionException {
		switch((i+ 8) % 8) {
		case 0:
			return Direction.NORTH;
		case 1:
			return Direction.NORTH_EAST;
		case 2:
			return Direction.EAST;
		case 3:
			return Direction.SOUTH_EAST;
		case 4:
			return Direction.SOUTH;
		case 5:
			return Direction.SOUTH_WEST;
		case 6:
			return Direction.WEST;
		case 7:
			return Direction.NORTH_WEST;
		default:
			return Direction.NORTH;
		}
	}

	public static int directionToInt(Direction d)  throws GameActionException {
		switch(d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return 0;
		}
	}

	public static int[] getRobotCount(RobotController rc) throws GameActionException{
		int[] robotCount = new int[21];
		for (int i = 0; i < robotCount.length; i++) {
			robotCount[i] = rc.readBroadcast(MyConstants.ROBOT_COUNT_OFFSET + i);
		}

		return robotCount;
	}

	public static RobotType getRobotTypeToSpawn(RobotController rc) throws GameActionException{
		RobotType[] types = RobotType.values();
		return types[rc.readBroadcast(MyConstants.SPAWN_TYPE_OFFSET)];
	}

	// This method will attempt to spawn in the given direction (or as close to it as possible)
	public static void trySpawn(RobotController rc, Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = Util.directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canSpawn(Util.directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(Util.directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}


	public static boolean attack(RobotController rc, RobotInfo[] enemyRobots) throws GameActionException{
		if (rc.isWeaponReady() && enemyRobots.length > 0) {
			MapLocation myLocation = rc.getLocation();
			RobotInfo toAttack = enemyRobots[0];
			int closest = Integer.MAX_VALUE;

			for (RobotInfo enemy : enemyRobots) {
				int distanceToEnemy = myLocation.distanceSquaredTo(enemy.location);
				if (distanceToEnemy < closest) {
					closest = distanceToEnemy;
					toAttack = enemy;
				} else if (distanceToEnemy == closest) {
					if (enemy.health < toAttack.health) {
						toAttack = enemy;
					}
				}
			}

			if (rc.canAttackLocation(toAttack.location)) {
				rc.attackLocation(toAttack.location);
				return true;
			}
		}
		return false;
	}
	public static ArrayList<MapLocation> calcMLine(RobotController rc, MapLocation goal) throws GameActionException {
		Direction dirToGoal;
		ArrayList<MapLocation> mLine = new ArrayList<MapLocation>();
		MapLocation previousLocation = rc.getLocation();

		while (!previousLocation.equals(goal)) {
			mLine.add(previousLocation);
			dirToGoal = previousLocation.directionTo(goal);
			previousLocation = previousLocation.add(dirToGoal);
		}

		return mLine;
	}

	public static MapLocation smartAttack(RobotController rc, int[] targetPriority, RobotInfo[] enemiesInRange) throws GameActionException{
		RobotInfo primeTarget = null;
		double primeTargetValue = 0.0;


		for (RobotInfo enemy : enemiesInRange){
			int priority = targetPriority[enemy.type.ordinal()];
			double thisTarget = 0.0;
			double maxHealth = enemy.type.maxHealth;
			double remainingHealth = enemy.health;


			switch (priority){
			// ignore completely
			case 0:
				break;

				// Kill this target ASAP, usually reserved for missiles and computers
			case 1:
				if (postKillTarget(rc, enemy)){
					return enemy.location;
				}
				break;

				// Kill this target ASAP iff it is at low health, usually reserved for commanders, launchers, towers and HQ
			case 2:
				maxHealth = enemy.type.maxHealth;
				remainingHealth = enemy.health;
				if (remainingHealth < (1 / 3) * maxHealth){
					if (postKillTarget(rc, enemy)){
						return enemy.location;
					}
				} else {
					thisTarget = enemyValue(rc, enemy, priority);
				}
				break;

				// go by priority	
			default:
				thisTarget = enemyValue(rc, enemy, priority);
				break;
			}

			if (thisTarget >= primeTargetValue){
				primeTarget = enemy;
				primeTargetValue = thisTarget;
			}
		}

		if (primeTarget != null){
			return primeTarget.location;
		}	
		return null;
	} 

	private static double enemyValue(RobotController rc, RobotInfo enemy, int priority) {
		double thisTarget = 1.0;
		double maxHealth = enemy.type.maxHealth;
		double remainingHealth = enemy.health;

		maxHealth = enemy.type.maxHealth;
		double percentHealthRemaining = (remainingHealth / maxHealth);

		double weaponsDelay = enemy.weaponDelay;
		if (weaponsDelay < 1){
			weaponsDelay = 0;
		}
		double distance = (Math.sqrt(enemy.location.distanceSquaredTo(rc.getLocation()))) - Math.sqrt(enemy.type.attackRadiusSquared);

		// calc value of remaining health
		if (percentHealthRemaining <= (3.0 / 4.0)){
			thisTarget += 1;
			if (percentHealthRemaining <= (1.0 / 2.0)){
				thisTarget += 1;
			}
		}

		// calc value of weapons delay
		switch ((int)weaponsDelay){
		case 0:
			thisTarget += 0;
			break;
		case 1:
			thisTarget += 2;
			break;
		case 2:
			thisTarget += 0;
			break;
		default:
			break;	
		}

		// calc value of distance
		if (distance <= 1){
			if (distance <= 0){
				thisTarget += 1;
			}
		} else {
			thisTarget += 0;
		}

		// calc value of priority
		switch (priority){
		case 2:
			thisTarget += 4;
			break;
		case 3:
			thisTarget += 3;
			break;
		case 4:
			thisTarget += 2;
			break;
		case 5:
			thisTarget += 1;
			break;
		default:
			break;

		}

		return thisTarget;
	}

	private static boolean postKillTarget(RobotController rc, RobotInfo enemy) throws GameActionException {
		int numKillTargets = rc.readBroadcast(MyConstants.NUM_KILL_TARGETS_OFFSET);
		int offset = MyConstants.KILL_TARGETS_OFFSET;
		int thisID = 0;

		for (int i = 0; i < (numKillTargets * 2) + 2; i += 2){
			thisID = rc.readBroadcast(offset + i);
			// enemy has already been posted
			if (thisID == enemy.ID){
				int remainingHealth = rc.readBroadcast(offset + i + 1);
				// will it survive this turn?
				if (remainingHealth > 0){
					// change amount of damage it's taking
					if (enemy.type == RobotType.MISSILE){
						remainingHealth = remainingHealth - 1;
					} else {
						remainingHealth = (int)(remainingHealth - RobotPlayer.attackPower);
					}
					// post the damage you'll deal to it
					if (remainingHealth <= 0){
						rc.broadcast(offset + i + 1, 0);
					}
					return true;
				} else {
					return false;
				}
			}

			// enemy has not been posted
			if (thisID == 0){
				// calc remaining health
				int remainingHealth = (int)enemy.health;
				if (enemy.type == RobotType.MISSILE){
					remainingHealth = remainingHealth - 1;
				} else {
					remainingHealth = (int)(remainingHealth - RobotPlayer.attackPower);
				}
				// post the info
				numKillTargets = numKillTargets + 1;
				rc.broadcast(MyConstants.NUM_KILL_TARGETS_OFFSET, numKillTargets);
				rc.broadcast(offset + i, thisID);
				rc.broadcast(offset + i + 1, remainingHealth);
				return true;
			}
		}
		// enemy is already accounted for
		return false;
	}

	public static void attackByType(RobotController rc, RobotInfo[] enemies, RobotType[] targetTypes) throws GameActionException {
		if (enemies.length == 0) {
			return;
		}
		RobotInfo target = null;
		double lowestHealth = enemies[0].health;
		Boolean canKill = false;
		Boolean firstTarget = true;

		for (RobotType tType : targetTypes){
			for (RobotInfo enemy : enemies){
				if (enemy.type == tType){
					if (firstTarget){
						target = enemy;
						lowestHealth = enemy.health;
						firstTarget = false;
						break;
					}				
					if (enemy.health <= rc.getType().attackPower){
						target = enemy;
						canKill = true;
					} else if (enemy.health < lowestHealth) {
						target = enemy;
						lowestHealth = enemy.health;
					}
				}
			}
			if (canKill || target != null){
				break;
			}
		}

		if (target == null){
			target = enemies[0];
		}

		if (rc.canAttackLocation(target.location)){
			rc.attackLocation(target.location);
		}
	}

	public static void debug(RobotController rc, String msg) throws GameActionException {
		if (rc.getID() == 24905) {
			//System.out.println("DEBUG: " + msg);
		}
	}

	// function will implement splash damage into its attack. It will attack enemies within its actual attack range
	// if it can. If enemies are sensed but not in range it will try and attack a square that will damage the most
	// enemies with splash damage.
	public static void splashAttack(RobotController rc, RobotInfo[] nearbyEnemies) throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		MapLocation attackLocation = null;

		if (nearbyEnemies.length > 0){
			double topDamage = 0.0;

			for (RobotInfo enemy : nearbyEnemies){
				MapLocation barrageLocation = enemy.location;
				double damage = 0.0;
				if(rc.canAttackLocation(barrageLocation)){
					int numSplashedRobots = rc.senseNearbyRobots(enemy.location, 2, RobotPlayer.enemyTeam).length;
					damage = RobotPlayer.attackPower + ((1/2) * RobotPlayer.attackPower * numSplashedRobots);
				} else {
					Direction enemyToMe = enemy.location.directionTo(myLocation);
					barrageLocation = enemy.location.add(enemyToMe);
					if (rc.canAttackLocation(barrageLocation)){
						barrageLocation = enemy.location.add(enemyToMe);
						int numSplashedRobots = rc.senseNearbyRobots(barrageLocation, 2, RobotPlayer.enemyTeam).length;
						damage = ((1/2) * RobotPlayer.attackPower * numSplashedRobots);
					} else {
						enemyToMe = barrageLocation.directionTo(myLocation);
						barrageLocation = barrageLocation.add(enemyToMe);
						if (rc.canAttackLocation(barrageLocation)){
							int numSplashedRobots = rc.senseNearbyRobots(barrageLocation, 2, RobotPlayer.enemyTeam).length;
							damage = ((1/2) * RobotPlayer.attackPower * numSplashedRobots);
						}
					}
				}
				if (damage >= topDamage){
					topDamage = damage;
					attackLocation = barrageLocation;
				}
			}

		} 

		if (attackLocation == null){
			int randInt = rand.nextInt(8);
			Direction randDir = intToDirection(randInt);
			switch (randDir){
			case NORTH:
				attackLocation = myLocation.add(randDir, (int)Math.sqrt(RobotPlayer.attackRange) + 1);
				break;
			case SOUTH:
				attackLocation = myLocation.add(randDir, (int)Math.sqrt(RobotPlayer.attackRange) + 1);
				break;
			case EAST:
				attackLocation = myLocation.add(randDir, (int)Math.sqrt(RobotPlayer.attackRange) + 1);
				break;
			case WEST:
				attackLocation = myLocation.add(randDir, (int)Math.sqrt(RobotPlayer.attackRange) + 1);
				break;

			default:
				attackLocation = myLocation.add(randDir, (int)Math.sqrt(RobotPlayer.attackRange));
				break;
			}
		}


		if (rc.canAttackLocation(attackLocation)){
			rc.attackLocation(attackLocation);
		} 
	}

	public static int reSupply(RobotController rc) throws GameActionException {
		int offset = MyConstants.MINER_INFO;
		int roundNum = Clock.getRoundNum();
		int i = 0;
		Boolean flag = true;

		while (flag){
			int value = rc.readBroadcast(offset + i);
			int state = rc.readBroadcast(offset + i + 1);
			debug(rc, "CHANNEL/STATE " + (offset+i) + " " + state);
			if (value == 0 || i > 100){
				flag = false;
			} else if (value == roundNum){
				if (state == 1){
					return offset + i + 2;
				}
			} else {
				i = i + 4;
			}			
		}

		return 0;
	}

	public static void requestSupply(RobotController rc, int requestAmount, MapLocation location) throws GameActionException {
		if (location.distanceSquaredTo(RobotPlayer.myHq) <= 15){
			int offset = MyConstants.REQUEST_HQ_SUPPLIES_OFFSET;
			int i = 0;
			Boolean flag = true;

			while(flag){
				int value = rc.readBroadcast(offset + i);
				if (value == 0){
					rc.broadcast(offset + i, requestAmount);
					rc.broadcast(offset + i + 1, location.x);
					rc.broadcast(offset + i + 2, location.y);
					return;
				} 
				i += 3;
			}
		}	
	}



	// This method looks at the surrounding tiles of the location sent to it, and compares how many void tiles there are to non-void tiles.
	// if there are more void tiles than non-void, it returns true, meaning it's a choke-point, aka a "tight butthole".
	// if there are more non-void tiles than void, it returns false, meaning it's not a choke-point, aka "dom's butthole" (loose).

	//don't edit, is used for trybuild. make new method for choke point if necessary
	public static boolean tightButtHole(RobotController rc, MapLocation location) throws GameActionException {
		MapLocation[] area = MapLocation.getAllMapLocationsWithinRadiusSq(location, 4);
		int voids = 0;
		int nonVoids = 0;
		for(int i = 0, len = area.length; i < len; i+= 2) {
			TerrainTile tile = rc.senseTerrainTile(area[i]);
			if(tile == TerrainTile.VOID || tile == TerrainTile.OFF_MAP) {
				voids++;
			}
			else {
				nonVoids++;
			}
		}
		if(voids <= nonVoids){
			return false;
		}
		else{
			return true;
		}
	}



	//UTILITY FUNCS

	/*
	 * returns true if someone can hit me at my current location
	 *
	 * broadcast - should i broadcast the enemies location as a temporary goal
	 */
	public static boolean shouldFlee(RobotController rc, MapLocation loc, boolean broadcast) throws GameActionException {
		for (RobotInfo robot: RobotPlayer.enemyRobots) {
			Direction dir = robot.location.directionTo(loc);
//			//System.out.println("dir: " + dir + ", +1: " + intToDirection(directionToInt(dir) + 1) + ", -1: " + intToDirection(directionToInt(dir) - 1));
			if (loc.distanceSquaredTo(robot.location.add(dir)) <= robot.type.attackRadiusSquared) {
				return true;
			}
			if (loc.distanceSquaredTo(robot.location.add(intToDirection(directionToInt(dir) + 1))) <= robot.type.attackRadiusSquared) {
				return true;
			}
			if (loc.distanceSquaredTo(robot.location.add(intToDirection(directionToInt(dir) - 1))) <= robot.type.attackRadiusSquared) {
				return true;
			}
		}

		//towers too
		for (MapLocation tloc: RobotPlayer.enemyTowers) {
			if (loc.distanceSquaredTo(tloc) <= RobotType.TOWER.attackRadiusSquared) {
				return true;
			}
		}

		// AND DAT HQ
		if (loc.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
			return true;
		}

		return false;
	}

	public static boolean shouldFleeFromUnits(RobotController rc, MapLocation loc, boolean broadcast) throws GameActionException {
		for (RobotInfo robot: RobotPlayer.enemyRobots) {
			Direction dir = robot.location.directionTo(loc);
//			//System.out.println("dir: " + dir + ", +1: " + intToDirection(directionToInt(dir) + 1) + ", -1: " + intToDirection(directionToInt(dir) - 1));
			if (loc.distanceSquaredTo(robot.location.add(dir)) <= robot.type.attackRadiusSquared) {
				return true;
			}
			if (loc.distanceSquaredTo(robot.location.add(intToDirection(directionToInt(dir) + 1))) <= robot.type.attackRadiusSquared) {
				return true;
			}
			if (loc.distanceSquaredTo(robot.location.add(intToDirection(directionToInt(dir) - 1))) <= robot.type.attackRadiusSquared) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Finds a location where no enemies can attack
	 *
	 *
	 * if no such location exists we just battle it out
	 */
	public static Direction fleeNew(RobotController rc) throws GameActionException {
		MapLocation desiredLoc;
		Util.debug(rc, "in flee ");


		dirLoop:
			for (Direction d: RobotPlayer.fastDirections) {
				if (d.equals(Direction.OMNI) || d.equals(Direction.NONE)) {
					continue;
				}

				//if i can move in the given direction
				if (rc.canMove(d)) {
					desiredLoc = RobotPlayer.myLocation.add(d);
					if (shouldFlee(rc, desiredLoc, false)) {
						continue;
					}

					Util.debug(rc, "no one can hit " + d.toString());
					return d;
				}
			}
		Util.debug(rc, "no where to flee, head home ");
		return RobotPlayer.myLocation.directionTo(RobotPlayer.myHq);
	}

	public static RobotInfo shareSupply(RobotController rc, RobotInfo[] nearbyAllies) throws GameActionException{
		double mySupply = rc.getSupplyLevel();

		for (RobotInfo ally : nearbyAllies){
			if (ally.supplyLevel < ((1.0 / 2.0) * mySupply)){
				return ally;
			}
		}
		return null;
	}

	public static MapLocation defendHQ(RobotController rc, int numContainingEnemies) throws GameActionException {
		MapLocation enemyLocation = null;
		
		int rand = RobotPlayer.rand.nextInt(numContainingEnemies);
		rand = rand * 2;
		int xCoord = rc.readBroadcast(MyConstants.CONTAINING_ENEMY_LOCATION_OFFSET + rand);
		int yCoord = rc.readBroadcast(MyConstants.CONTAINING_ENEMY_LOCATION_OFFSET + rand + 1);
		enemyLocation = new MapLocation(xCoord, yCoord);
		
		return enemyLocation;
	}

	public static void yoloBitches(RobotController rc) throws GameActionException {
		if (rc.isWeaponReady()) {
			// RobotController rc, int myRange, Team enemyTeam
			attackSomething(rc, RobotPlayer.attackRange, RobotPlayer.enemyTeam);
		}

		Direction d = Bug.startBuggin(rc, RobotPlayer.enemyHq, 0);
		Direction dirToDest = RobotPlayer.myLocation.directionTo(RobotPlayer.enemyHq);
		if (!safeToMoveTo(rc, RobotPlayer.myLocation.add(dirToDest))) {
			// FUCK TOWERS AND HQ, move there!!!
			if (rc.isCoreReady()) {
				if (rc.canMove(dirToDest)){
					rc.move(dirToDest);
				} else if (rc.canMove(d)) {
					rc.move(d);
				}
			}
		} else {
			if (rc.isCoreReady() && rc.canMove(d)) {
				rc.move(d);
			}
		}
	}
}








