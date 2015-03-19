package team302;

import battlecode.common.*;

import java.lang.System;
import java.util.*;

public class HQ {
	public static RobotController rc;
	public static RobotType[] canSpawn = {RobotType.BEAVER};
	public static Boolean first = true;
	public static Boolean second = false;
	static int avgOre = 0;
	static int numMiners;
	static double enemyHqHp = 2000;
	static double enemyTowersHealth[] = new double[6];
	static boolean winning = false;
	static int roundLimit;
	static boolean soldierStrat = false;
	static boolean tankStrat = true;

	public static void execute(RobotController rc_in) throws GameActionException {
		rc = rc_in;
		RobotPlayer.targets = MyConstants.assaultPriorities;
		int executeStartRound = Clock.getRoundNum();
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, RobotPlayer.myTeam);
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(999999, RobotPlayer.enemyTeam);

		assessTheSituation(myRobots, enemyRobots);
		checkIfContained(rc, enemyRobots);


		if (second){
			int target = MyConstants.AVG_ORE;
			avgOre = (rc.readBroadcast(target) / RobotPlayer.myTowers.length);
			numMiners = avgOre/10 + 20;
			if(numMiners > 35){
				numMiners = 35;
			}

			double offset = (roundLimit - 2000)/2;
			offset = 1 + (offset / 1000);
			double dubMiners = (double)numMiners * offset;
			numMiners = (int)dubMiners;

			if(RobotPlayer.myHq.distanceSquaredTo(RobotPlayer.enemyHq) > 1600){
				//System.out.println("LONG");
				numMiners = numMiners/2;
				numMiners = numMiners * 3;
			}

			second = false;
			RobotPlayer.myTowers = rc.senseTowerLocations();
			RobotPlayer.numInitialTowers = RobotPlayer.myTowers.length;
		}

		if (first) {
			first = false;
			second = true;
			rc.broadcast(MyConstants.MOST_ORE_LOCATION_X, RobotPlayer.myHq.x);
			rc.broadcast(MyConstants.MOST_ORE_LOCATION_Y, RobotPlayer.myHq.y);
			rc.broadcast(MyConstants.MOST_ORE, 0);

			rc.broadcast(MyConstants.MAX_BARRACKS_OFFSET, 3);
			rc.broadcast(MyConstants.MAX_TANK_FACTORIES_OFFSET, 2);
			rc.broadcast(MyConstants.MAX_SUPPLY_DEPOTS_OFFSET, 5);

			RobotPlayer.initialEnemyTowerLocations = RobotPlayer.enemyTowers;
			Arrays.fill(enemyTowersHealth, 1000.0);
			roundLimit = rc.getRoundLimit();
		}

		//reset most ore location
		if (Clock.getRoundNum() % 25 == 0){
			rc.broadcast(MyConstants.MOST_ORE, 0);
		}


		if (rc.isCoreReady()) {
			Util.spawnWithPrecedence(rc, Util.intToDirection(RobotPlayer.rand.nextInt(4) * 2), canSpawn);

			if (rc.isWeaponReady()){
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(RobotPlayer.sensorRange, RobotPlayer.enemyTeam);
				if (RobotPlayer.myTowers.length >= 5){
					Util.splashAttack(rc_in, nearbyEnemies);
				} else {
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
		}
		transferSupply();
	}

	private static void checkIfContained(RobotController rc, RobotInfo[] enemyRobots) throws GameActionException {
		rc.broadcast(MyConstants.NUM_CONTAINING_ENEMIES_OFFSET, 0);
		int numCloseRobots = 0;
		int offset = MyConstants.CONTAINING_ENEMY_LOCATION_OFFSET;

		for (RobotInfo enemy : enemyRobots){
			if (enemy.location.distanceSquaredTo(RobotPlayer.myHq) <= 100){
				
				if (enemy.type.equals(RobotType.COMMANDER)){
					rc.broadcast(MyConstants.NUM_CONTAINING_ENEMIES_OFFSET, 1);
					rc.broadcast(MyConstants.CONTAINING_ENEMY_LOCATION_OFFSET, enemy.location.x);
					rc.broadcast(MyConstants.CONTAINING_ENEMY_LOCATION_OFFSET + 1, enemy.location.y);
					return;
				}	
				numCloseRobots += 1;
				rc.broadcast(offset, enemy.location.x);
				rc.broadcast(offset + 1, enemy.location.y);
				offset += 2;
			}	
		}

		if (numCloseRobots >= 3){
			rc.broadcast(MyConstants.NUM_CONTAINING_ENEMIES_OFFSET, numCloseRobots);
		} else {
			rc.broadcast(MyConstants.NUM_CONTAINING_ENEMIES_OFFSET, 0);	
		}
	}

	private static void transferSupply() throws GameActionException {
		int offset = 0;
		int channel = MyConstants.REQUEST_HQ_SUPPLIES_OFFSET;
		int numSupply = rc.readBroadcast(channel);
		// loop until we hit a zero, meaning there are no more requests
		while (numSupply != 0) {
			// only do transfer things if we have enough bytecodes
			if (Clock.getBytecodesLeft() > 600) {
				MapLocation transferLocation = new MapLocation(rc.readBroadcast(channel + 1), rc.readBroadcast(channel + 2));

				if (rc.canSenseLocation(transferLocation)) {
					RobotInfo ri = rc.senseRobotAtLocation(transferLocation);
					if (ri != null && numSupply - ri.supplyLevel > 300) {
						rc.transferSupplies(numSupply - (int)ri.supplyLevel, transferLocation);
					}
				}
			}
			// reset channel for next run
			rc.broadcast(channel, 0);
			offset += 3;
			channel = MyConstants.REQUEST_HQ_SUPPLIES_OFFSET + offset;
			numSupply = rc.readBroadcast(channel);
		}
	}


	/*
	 * counts how many of each type of robot there are on the given team
	 */
	public static int[] countTypes(RobotInfo[] robots) throws GameActionException {
		int[] typeCount = new int[21];
		for (RobotInfo r : robots) {
			RobotType rt = r.type;

			// The max amount of supply we want any robot to acquire from the HQ
			int maxRobotSupply = 500;

			switch (rt) {
			case AEROSPACELAB:
				typeCount[RobotType.AEROSPACELAB.ordinal()]++;
				break;
			case BARRACKS:
				typeCount[RobotType.BARRACKS.ordinal()]++;
				break;
			case BASHER:
				typeCount[RobotType.BASHER.ordinal()]++;
				break;
			case BEAVER:
				typeCount[RobotType.BEAVER.ordinal()]++;
				break;
			case COMMANDER:
				typeCount[RobotType.COMMANDER.ordinal()]++;
				break;
			case COMPUTER:
				typeCount[RobotType.COMPUTER.ordinal()]++;
				break;
			case DRONE:
				typeCount[RobotType.DRONE.ordinal()]++;
				break;
			case HANDWASHSTATION:
				typeCount[RobotType.HANDWASHSTATION.ordinal()]++;
				break;
			case HELIPAD:
				typeCount[RobotType.HELIPAD.ordinal()]++;
				break;
			case HQ:
				typeCount[RobotType.HQ.ordinal()]++;
				break;
			case LAUNCHER:
				typeCount[RobotType.LAUNCHER.ordinal()]++;
				break;
			case MINER:
				typeCount[RobotType.MINER.ordinal()]++;
				break;
			case MINERFACTORY:
				typeCount[RobotType.MINERFACTORY.ordinal()]++;
				break;
			case MISSILE:
				typeCount[RobotType.MISSILE.ordinal()]++;
				break;
			case SOLDIER:
				typeCount[RobotType.SOLDIER.ordinal()]++;
				break;
			case SUPPLYDEPOT:
				typeCount[RobotType.SUPPLYDEPOT.ordinal()]++;
				break;
			case TANK:
				typeCount[RobotType.TANK.ordinal()]++;
				break;
			case TANKFACTORY:
				typeCount[RobotType.TANKFACTORY.ordinal()]++;
				break;
			case TECHNOLOGYINSTITUTE:
				typeCount[RobotType.TECHNOLOGYINSTITUTE.ordinal()]++;
				break;
			case TOWER:
				typeCount[RobotType.TOWER.ordinal()]++;
				break;
			case TRAININGFIELD:
				typeCount[RobotType.TRAININGFIELD.ordinal()]++;
				break;
			}
		}
		typeCount[RobotType.HQ.ordinal()] = 1;

		for (int i = 0; i < typeCount.length; i++) {
			rc.broadcast(MyConstants.ROBOT_COUNT_OFFSET + i, typeCount[i]);
		}

		return typeCount;
	}

	public static void assessTheSituation(RobotInfo[] myRobots, RobotInfo[] enemyRobots) throws GameActionException{
		int[] allyTypeCount = countTypes(myRobots);

		//		int roundNum = Clock.getRoundNum();
		//		if (roundNum > 700 && roundNum % 100 == 0) {
		//			determineEnemyStrat(enemyRobots);
		//		}
		int x = 0;
		int y = 0;
		int count = 0;
		for (RobotInfo r: myRobots) {
			switch(r.type) {
			case TANK:
				count++;
				x += r.location.x;
				y += r.location.y;
			default:
				continue;
			}
		}

		if (count != 0) {
			x = x / count;
			y = y / count;
		}


		//		int roundNum = Clock.getRoundNum();
		//		if (roundNum > 700 && roundNum % 100 == 0) {
		//			determineEnemyStrat(enemyRobots);
		//		}
		broadcastNextSpawnType(allyTypeCount);
		broadcastNextAttackLocation(allyTypeCount[RobotType.TANK.ordinal()], new MapLocation(x,y));
		ReportSwarms(multiGroupCollapse());
	}

	// this function broadcasts the number to spawn of a given type if we have less of that robot type than numDesired
	/*
	 * allyTypeCount: an array containing how many of each robot type we have
	 * type: the RobotType we want to spawn/build
	 * numDesired: how many we want
	 * oreRemaining: how much ore we have left
	 * limit: our bottleneck on building that robot (e.g. if we want to spawn miners and only have 2 miner factories the limit is 2)
	 */
	public static double spawningRule(int[] allyTypeCount, RobotType type, int numDesired, double oreRemaining, int limit) throws GameActionException {
		if (allyTypeCount[type.ordinal()] < numDesired) {
			if(type.oreCost > oreRemaining){
				return 0;
			}
			//we want to spawn numDesired - our robot count for the given type
			//however we could be limited by the number of builder structures we have
			// so we take the smaller of limit and numDesired - our robot count for the given type
			int numToSpawn = Math.min(numDesired - allyTypeCount[type.ordinal()], limit);
			rc.broadcast(MyConstants.SPAWN_TYPE_OFFSET + type.ordinal(), numToSpawn);
			oreRemaining = oreRemaining - (type.oreCost * numToSpawn);
			////System.out.println("Spawning " + numToSpawn + " " + type.toString() + " " + oreRemaining + " ore remaining");
			allyTypeCount[type.ordinal()] -= numToSpawn;
		}

		return oreRemaining;
	}

	//set the spawning precedence here
	// 10 ore avg: 8 miners
	// 20 ore avg: 13 miners
	// 30 ore avg: 19 miners
	// 40 ore avg: 25 miners
	public static void broadcastNextSpawnType(int[] allyTypeCount) throws GameActionException {
		double remainingOre = rc.getTeamOre();
		int roundsLeft = roundLimit - Clock.getRoundNum();
		if (roundsLeft < 240 && roundsLeft > 100) {
			remainingOre = spawningRule(allyTypeCount, RobotType.HANDWASHSTATION, 999, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
			remainingOre = spawningRule(allyTypeCount, RobotType.BEAVER, 87591, remainingOre, allyTypeCount[RobotType.HQ.ordinal()]);
		}

		remainingOre = spawningRule(allyTypeCount, RobotType.BEAVER, 1, remainingOre, allyTypeCount[RobotType.HQ.ordinal()]);
		if (remainingOre < 0) return;
		remainingOre = spawningRule(allyTypeCount, RobotType.MINERFACTORY, 1, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;
//		remainingOre = spawningRule(allyTypeCount, RobotType.TECHNOLOGYINSTITUTE, 1, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
//		if (remainingOre < 0) return;
//		if (allyTypeCount[RobotType.TRAININGFIELD.ordinal()] == 0 && rc.checkDependencyProgress(RobotType.TECHNOLOGYINSTITUTE) == DependencyProgress.DONE) {
//			remainingOre = spawningRule(allyTypeCount, RobotType.TRAININGFIELD, 1, remainingOre, allyTypeCount[RobotType.TECHNOLOGYINSTITUTE.ordinal()]);
//			if (remainingOre < 0) return;
//		}
//		remainingOre = spawningRule(allyTypeCount, RobotType.COMMANDER, 1, remainingOre, allyTypeCount[RobotType.TRAININGFIELD.ordinal()]);
//		if (remainingOre < 0) return;


		remainingOre = spawningRule(allyTypeCount, RobotType.MINER, (numMiners / 2), remainingOre, allyTypeCount[RobotType.MINERFACTORY.ordinal()]);
		if (remainingOre < 0) return;
		if (Clock.getRoundNum() > 400){
			remainingOre = spawningRule(allyTypeCount, RobotType.MINER, numMiners, remainingOre, allyTypeCount[RobotType.MINERFACTORY.ordinal()]);
			if (remainingOre < 0) return;
		}
		remainingOre = spawningRule(allyTypeCount, RobotType.BEAVER, 3, remainingOre, allyTypeCount[RobotType.HQ.ordinal()]);
		if (remainingOre < 0) return;
		// need a barracks regardless
		remainingOre = spawningRule(allyTypeCount, RobotType.BARRACKS, 1, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;
		remainingOre = spawningRule(allyTypeCount, RobotType.SUPPLYDEPOT, 4, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;
		
		remainingOre = spawningRule(allyTypeCount, RobotType.TANKFACTORY, rc.readBroadcast(MyConstants.MAX_TANK_FACTORIES_OFFSET), remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;
		remainingOre = spawningRule(allyTypeCount, RobotType.TANK, 99999999, remainingOre, allyTypeCount[RobotType.TANKFACTORY.ordinal()]);
		if (remainingOre < 0) return;
		
		remainingOre = spawningRule(allyTypeCount, RobotType.HELIPAD, 1, remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;
		remainingOre = spawningRule(allyTypeCount, RobotType.DRONE, 6, remainingOre, allyTypeCount[RobotType.HELIPAD.ordinal()]);
		if (remainingOre < 0) return;


		remainingOre = spawningRule(allyTypeCount, RobotType.SUPPLYDEPOT,  rc.readBroadcast(MyConstants.MAX_SUPPLY_DEPOTS_OFFSET), remainingOre, allyTypeCount[RobotType.BEAVER.ordinal()]);
		if (remainingOre < 0) return;

		if (remainingOre > 1000){
			rc.broadcast(MyConstants.MAX_TANK_FACTORIES_OFFSET, rc.readBroadcast(MyConstants.MAX_TANK_FACTORIES_OFFSET) + 1);
			rc.broadcast(MyConstants.MAX_SUPPLY_DEPOTS_OFFSET, rc.readBroadcast(MyConstants.MAX_SUPPLY_DEPOTS_OFFSET) + 3);
		}
	}

	public static void broadcastNextAttackLocation(int tanks, MapLocation tankLoc) throws GameActionException{
		int offSet = MyConstants.MAIN_GOAL;
		int[] safeArray = new int[RobotPlayer.enemyTowers.length];

		if (RobotPlayer.enemyTowers.length > 3 || tanks < 20) {
			for (int i = 0; i < RobotPlayer.enemyTowers.length; i++) {
				MapLocation thisTower = RobotPlayer.enemyTowers[i];
				int numTowersInRange = 0;

				if (thisTower.distanceSquaredTo(RobotPlayer.enemyHq) <= RobotType.HQ.attackRadiusSquared) {
					numTowersInRange += 1;
				}

				int numVoidTiles = 0;
				TerrainTile adjacent;
				for (Direction dir : Direction.values()){
					adjacent = rc.senseTerrainTile(thisTower.add(dir));
					if (adjacent.equals(TerrainTile.VOID)){
						numVoidTiles += 1;
					}
				}
				if (numVoidTiles > 1){
					numTowersInRange += 1;
				}


				// for each other tower
				for (MapLocation towerLoc : RobotPlayer.enemyTowers) {
					// can hit hit the base of thisTower
					if (!towerLoc.equals(thisTower)) {
						if (towerLoc.distanceSquaredTo(thisTower) <= RobotType.TOWER.attackRadiusSquared) {
							numTowersInRange += 1;
						}
					}
				}

				if (numTowersInRange < 1) {
					safeArray[i] = 1;
				}else if (numTowersInRange < 2) {
					safeArray[i] = 2;
				}else if (numTowersInRange < 3) {
					safeArray[i] = 3;
				}else {
					safeArray[i] = 4;
				}
			}

			double closest = 99999;
			MapLocation goal = null;
			double distToEnemy;
			for (int i = 0; i < safeArray.length; i++) {
				distToEnemy = RobotPlayer.enemyTowers[i].distanceSquaredTo(tankLoc);
				if (safeArray[i] == 1 && distToEnemy < closest) {
					goal = RobotPlayer.enemyTowers[i];
					closest = distToEnemy;
				}
			}

			if (goal != null) {
				rc.broadcast(offSet, goal.x);
				rc.broadcast(offSet + 1, goal.y);
				rc.broadcast(offSet + 4, RobotType.TOWER.ordinal());
				return;
			}

			for (int i = 0; i < safeArray.length; i++) {
				distToEnemy = RobotPlayer.enemyTowers[i].distanceSquaredTo(tankLoc);
				if (safeArray[i] == 2 && distToEnemy < closest) {
					goal = RobotPlayer.enemyTowers[i];
					closest = distToEnemy;
				}
			}

			if (goal != null) {
				rc.broadcast(offSet, goal.x);
				rc.broadcast(offSet + 1, goal.y);
				rc.broadcast(offSet + 4, RobotType.TOWER.ordinal());
				return;
			}

			for (int i = 0; i < safeArray.length; i++) {
				distToEnemy = RobotPlayer.enemyTowers[i].distanceSquaredTo(tankLoc);
				if (safeArray[i] == 3 && distToEnemy < closest) {
					goal = RobotPlayer.enemyTowers[i];
					closest = distToEnemy;
				}
			}

			if (goal != null) {
				rc.broadcast(offSet, goal.x);
				rc.broadcast(offSet + 1, goal.y);
				rc.broadcast(offSet + 4, RobotType.TOWER.ordinal());
				return;
			}

			for (int i = 0; i < safeArray.length; i++) {
				distToEnemy = RobotPlayer.enemyTowers[i].distanceSquaredTo(tankLoc);
				if (safeArray[i] == 4 && distToEnemy < closest) {
					goal = RobotPlayer.enemyTowers[i];
					closest = distToEnemy;
				}
			}

			if (goal != null) {
				rc.broadcast(offSet, goal.x);
				rc.broadcast(offSet + 1, goal.y);
				rc.broadcast(offSet + 4, RobotType.TOWER.ordinal());
				return;
			}
		}
		rc.broadcast(offSet, RobotPlayer.enemyHq.x);
		rc.broadcast(offSet + 1, RobotPlayer.enemyHq.y);
		rc.broadcast(offSet + 4, RobotType.HQ.ordinal());
	}

	public static void ReportSwarms(boolean multiGroupCollapse) throws GameActionException {
		int x,y, total_power, total_health;
		double roundGoalDies, roundsGroupDies;
		MapLocation goal;
		RobotInfo goalInfo;
		RobotType goalType;
		int roundsRemaining = rc.getRoundLimit() - Clock.getRoundNum();
		int currOffset = MyConstants.GROUP_OFFSET;
		while (rc.readBroadcast(currOffset) != 0) {
			if (roundsRemaining < 100) {
				rc.broadcast(currOffset + GroupConstants.COLLAPSE, 1);
				currOffset += 14;
				continue;
			}


			x = rc.readBroadcast(currOffset + GroupConstants.CURRENT_ATTACK_LOCATION);
			y = rc.readBroadcast(currOffset + GroupConstants.CURRENT_ATTACK_LOCATION + 1);
			if (x==0 && y==0) {
				rc.broadcast(currOffset + GroupConstants.COLLAPSE, 0);
			} else {
				goal = new MapLocation(x, y);
				if (multiGroupCollapse) {
					int mgX = rc.readBroadcast(MyConstants.MAIN_GOAL);
					int mgY = rc.readBroadcast(MyConstants.MAIN_GOAL + 1);
					MapLocation mgLoc = new MapLocation(mgX, mgY);

					if (mgLoc.equals(goal)) {
						rc.broadcast(currOffset + GroupConstants.COLLAPSE, 1);
						rc.broadcast(currOffset + GroupConstants.SWARM_POWER, 0);
						rc.broadcast(currOffset + GroupConstants.SWARM_HEALTH, 0);
						currOffset += 14;
						continue;
					}
				}


				total_power = rc.readBroadcast(currOffset + GroupConstants.SWARM_POWER);
				total_health = rc.readBroadcast(currOffset + GroupConstants.SWARM_HEALTH);
				//				//System.out.println("0 myatt/myhealth " + total_power + " " + total_health + " "  + currOffset);

				if (total_health !=0 && total_power != 0) {
					if (rc.canSenseLocation(goal)) {
						goalInfo = rc.senseRobotAtLocation(goal);
						if (goalInfo == null) {
							rc.broadcast(currOffset + GroupConstants.COLLAPSE, 0);
							rc.broadcast(currOffset + GroupConstants.SWARM_POWER, 0);
							rc.broadcast(currOffset + GroupConstants.SWARM_HEALTH, 0);
							currOffset += 14;
							continue;
						}
						goalType = goalInfo.type;
						roundGoalDies = goalInfo.health / total_power;
					} else {
						goalType = RobotType.values()[rc.readBroadcast(currOffset + GroupConstants.CURRENT_ATTACK_TYPE)];
						roundGoalDies = goalType.maxHealth / total_power;
					}

					roundsGroupDies = total_health / (goalType.attackPower / goalType.attackDelay);
					//					//System.out.println("1 myatt/myhealth " + total_power + " " + total_health);

					if (roundsGroupDies > roundGoalDies) {
						rc.broadcast(currOffset + GroupConstants.COLLAPSE, 1);
					} else {
						rc.broadcast(currOffset + GroupConstants.COLLAPSE, 0);
					}
				} else {
					rc.broadcast(currOffset + GroupConstants.COLLAPSE, 0);
				}
			}
			rc.broadcast(currOffset + GroupConstants.SWARM_POWER, 0);
			rc.broadcast(currOffset + GroupConstants.SWARM_HEALTH, 0);
			currOffset += 14;
		}

	}

	public static boolean multiGroupCollapse() throws GameActionException {
		int roundsRemaining = rc.getRoundLimit() - Clock.getRoundNum();
		if (roundsRemaining < 100) {
			return true;
		}

		double roundGoalDies, roundsGroupDies;
		int x = rc.readBroadcast(MyConstants.MAIN_GOAL);
		int y = rc.readBroadcast(MyConstants.MAIN_GOAL + 1);
		MapLocation commoneGoal = new MapLocation(x,y);
		RobotInfo goalInfo;
		RobotType goalType;
		int power = rc.readBroadcast(MyConstants.MAIN_GOAL + 2);
		int health = rc.readBroadcast(MyConstants.MAIN_GOAL + 3);

		rc.broadcast(MyConstants.MAIN_GOAL + 2, 0);
		rc.broadcast(MyConstants.MAIN_GOAL + 3, 0);

		if (power == 0 && health == 0) {
			return false;
		} else {

			if (rc.canSenseLocation(commoneGoal)) {
				goalInfo = rc.senseRobotAtLocation(commoneGoal);
				if (goalInfo == null) {
					return false;
				}
				goalType = goalInfo.type;
				roundGoalDies = goalInfo.health / power;
			} else {
				goalType = RobotType.values()[rc.readBroadcast(MyConstants.MAIN_GOAL + 4)];
				roundGoalDies = goalType.maxHealth / power;
			}
			roundsGroupDies = health / (goalType.attackPower / goalType.attackDelay);
			if (roundsGroupDies > roundGoalDies) {
				return true;
			}
		}
		return false;
	}


	// determines if we are winning. Takes 471 bytecodes in the worst case scenario
	public static boolean winning() throws GameActionException {
		int myNumTowers = 0;
		int myTotalTowerHp = 0;
		int currentRoundNum = Clock.getRoundNum();
		int nextTimeStamp;
		for  (int i = 0, len = RobotPlayer.numInitialTowers; i < len; i++) {
			nextTimeStamp = rc.readBroadcast(MyConstants.MY_TOWER_HEALTH + (i*2));
			if (currentRoundNum - nextTimeStamp < 2) {
				myTotalTowerHp += rc.readBroadcast(MyConstants.MY_TOWER_HEALTH + (i*2) + 1);
				myNumTowers++;
			}
		}

		// --------------------------------
		// 1. Number of towers remaining
		// --------------------------------
		if (RobotPlayer.enemyTowers.length > myNumTowers) {
			//			//System.out.println("Less Towers: " + RobotPlayer.enemyTowers.length + " " + myNumTowers);
			return false;
		} else if (RobotPlayer.enemyTowers.length < myNumTowers) {
			//			//System.out.println("More Towers: " + RobotPlayer.enemyTowers.length + " " + myNumTowers);
			return true;
		}

		// --------------------------------
		// 2. HQ HP remaining
		// --------------------------------
		if (rc.canSenseLocation(RobotPlayer.enemyHq)) {
			enemyHqHp = rc.senseRobotAtLocation(RobotPlayer.enemyHq).health;
		}
		double myHqHp = rc.getHealth();
		if (enemyHqHp > myHqHp) {
			//			//System.out.println("Less HQ Health: " + enemyHqHp + " " + myHqHp);
			return false;
		} else if (enemyHqHp < myHqHp) {
			//			//System.out.println("More HQ Health: " + enemyHqHp + " " + myHqHp);
			return true;
		}

		// --------------------------------
		// 3. Total HP of towers
		// --------------------------------
		int enenmyTotalTowerHp = 0;
		for (int i = 0, len = RobotPlayer.initialEnemyTowerLocations.length; i < len; i++) {
			if (rc.canSenseLocation(RobotPlayer.initialEnemyTowerLocations[i])) {
				RobotInfo enemyTower = rc.senseRobotAtLocation(RobotPlayer.initialEnemyTowerLocations[i]);
				if (enemyTower == null) {
					enemyTowersHealth[i] = 0;
				} else {
					enemyTowersHealth[i] = enemyTower.health;
				}
			}
			enenmyTotalTowerHp += enemyTowersHealth[i];
		}

		if (enenmyTotalTowerHp > myTotalTowerHp) {
			//			//System.out.println("Less Tower Health: " + enenmyTotalTowerHp + " " + myTotalTowerHp);
			return false;
		} else if (enenmyTotalTowerHp < myTotalTowerHp) {
			//			//System.out.println("More Tower Health: " + enenmyTotalTowerHp + " " + myTotalTowerHp);
			return true;
		}
		// 4. Number of handwash stations - CAN'T ACCURATELY CALCULATE
		// 5. Sum of ore stockpile plus ore costs of all surviving robots - CAN'T ACCURATELY CALCULATE
		// 6. Team HQ ID - DONT CARE ATE THIS POINT

		// just assume we are losing if we make it here
		//		//System.out.println("EVERYONE IS EQUAL!");
		return false;
	}

}