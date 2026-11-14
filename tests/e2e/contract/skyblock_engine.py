"""
Full reference simulation engine for expedition-skyblock.
Faithfully models all data structures, interface contracts, command handlers,
protection mechanisms, boundary containment, and dimensional mechanics.
"""
import copy
import json
import os
import tempfile
import time
from typing import Dict, List, Set, Tuple, Optional, Any
from .island_math import IslandMath, GridPos, WorldPos


class PlayerReturnLocation:
    def __init__(self, dimension: str, x: float, y: float, z: float, yaw: float = 0.0, pitch: float = 0.0):
        self.dimension = dimension
        self.x = x
        self.y = y
        self.z = z
        self.yaw = yaw
        self.pitch = pitch

    def to_dict(self) -> Dict[str, Any]:
        return {
            "dimension": self.dimension,
            "x": self.x,
            "y": self.y,
            "z": self.z,
            "yaw": self.yaw,
            "pitch": self.pitch
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "PlayerReturnLocation":
        return cls(
            dimension=data["dimension"],
            x=data["x"],
            y=data["y"],
            z=data["z"],
            yaw=data.get("yaw", 0.0),
            pitch=data.get("pitch", 0.0)
        )


class SkyblockIsland:
    def __init__(
        self,
        owner_uuid: str,
        owner_name: str,
        island_index: int,
        grid_x: int,
        grid_z: int,
        center_x: int,
        center_z: int,
        home_x: float,
        home_y: float,
        home_z: float,
        home_yaw: float = 0.0,
        home_pitch: float = 0.0,
        is_locked: bool = True,
        created_at: int = 0
    ):
        self.owner_uuid = owner_uuid
        self.owner_name = owner_name
        self.island_index = island_index
        self.grid_x = grid_x
        self.grid_z = grid_z
        self.center_x = center_x
        self.center_z = center_z
        self.home_x = home_x
        self.home_y = home_y
        self.home_z = home_z
        self.home_yaw = home_yaw
        self.home_pitch = home_pitch
        self.is_locked = is_locked
        self.created_at = created_at or int(time.time() * 1000)
        self.invited_players: Set[str] = set()

        # Simulated world blocks for this island: (x, y, z) -> block_id
        self.blocks: Dict[Tuple[int, int, int], str] = {}
        # Simulated starter chest contents: slot_index -> (item_id, count)
        self.chest_inventory: Dict[int, Tuple[str, int]] = {}

    def to_dict(self) -> Dict[str, Any]:
        return {
            "ownerUUID": self.owner_uuid,
            "ownerName": self.owner_name,
            "islandIndex": self.island_index,
            "gridX": self.grid_x,
            "gridZ": self.grid_z,
            "centerX": self.center_x,
            "centerZ": self.center_z,
            "homeX": self.home_x,
            "homeY": self.home_y,
            "homeZ": self.home_z,
            "homeYaw": self.home_yaw,
            "homePitch": self.home_pitch,
            "isLocked": self.is_locked,
            "createdAt": self.created_at,
            "invitedPlayers": sorted(list(self.invited_players))
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SkyblockIsland":
        island = cls(
            owner_uuid=data["ownerUUID"],
            owner_name=data.get("ownerName", "Player"),
            island_index=data["islandIndex"],
            grid_x=data["gridX"],
            grid_z=data["gridZ"],
            center_x=data["centerX"],
            center_z=data["centerZ"],
            home_x=data["homeX"],
            home_y=data["homeY"],
            home_z=data["homeZ"],
            home_yaw=data.get("homeYaw", 0.0),
            home_pitch=data.get("homePitch", 0.0),
            is_locked=data.get("isLocked", True),
            created_at=data.get("createdAt", 0)
        )
        island.invited_players = set(data.get("invitedPlayers", []))
        return island


class MockPlayer:
    def __init__(
        self,
        uuid: str,
        name: str,
        dimension: str = "minecraft:overworld",
        x: float = 0.0,
        y: float = 64.0,
        z: float = 0.0,
        yaw: float = 0.0,
        pitch: float = 0.0,
        is_op: bool = False
    ):
        self.uuid = uuid
        self.name = name
        self.dimension = dimension
        self.x = x
        self.y = y
        self.z = z
        self.yaw = yaw
        self.pitch = pitch
        self.vx = 0.0
        self.vy = 0.0
        self.vz = 0.0
        self.fall_distance = 0.0
        self.on_ground = True
        self.is_op = is_op

        # Combat tracker: timestamp of last damage taken
        self.last_damage_time = -1000.0
        self.health = 20.0
        self.is_alive = True

        # Inventory & XP
        self.inventory: List[Dict[str, Any]] = [
            {"id": "minecraft:iron_sword", "count": 1},
            {"id": "minecraft:diamond_pickaxe", "count": 1},
            {"id": "minecraft:cooked_beef", "count": 64}
        ]
        self.xp_level = 30
        self.total_xp = 1395
        self.xp_progress = 0.0
        self.score = 500

        # Message logs
        self.chat_messages: List[str] = []
        self.actionbar_messages: List[str] = []

    def send_system_message(self, message: str) -> None:
        self.chat_messages.append(message)

    def send_actionbar(self, message: str) -> None:
        self.actionbar_messages.append(message)

    def take_damage(self, amount: float, current_time: float) -> None:
        self.health = max(0.0, self.health - amount)
        self.last_damage_time = current_time
        if self.health <= 0:
            self.is_alive = False

    def reset_combat(self) -> None:
        self.last_damage_time = -1000.0

    def teleport(self, dimension: str, x: float, y: float, z: float, yaw: float = 0.0, pitch: float = 0.0) -> None:
        self.dimension = dimension
        self.x = x
        self.y = y
        self.z = z
        self.yaw = yaw
        self.pitch = pitch
        self.vx = 0.0
        self.vy = 0.0
        self.vz = 0.0
        self.fall_distance = 0.0


class SkyblockEngine:
    SKYBLOCK_DIMENSION: str = "expedition:skyblock"
    OVERWORLD_DIMENSION: str = "minecraft:overworld"
    NETHER_DIMENSION: str = "minecraft:the_nether"
    END_DIMENSION: str = "minecraft:the_end"

    def __init__(self, world_data_path: Optional[str] = None):
        self.world_data_path = world_data_path
        self.next_island_index = 0
        self.islands: Dict[str, SkyblockIsland] = {}  # owner_uuid -> Island
        self.player_returns: Dict[str, PlayerReturnLocation] = {}  # player_uuid -> ReturnLocation
        self.global_keep_inventory = False  # Vanilla gamerule in non-skyblock worlds
        self.current_time_seconds = 1000.0   # Simulated clock

    def advance_time(self, seconds: float) -> None:
        self.current_time_seconds += seconds

    # ==================== Island Allocation & Generation ====================

    def get_island_by_owner(self, owner_uuid: str) -> Optional[SkyblockIsland]:
        return self.islands.get(owner_uuid)

    def get_island_by_name(self, name: str) -> Optional[SkyblockIsland]:
        for island in self.islands.values():
            if island.owner_name.lower() == name.lower():
                return island
        return None

    def allocate_island(self, player: MockPlayer) -> SkyblockIsland:
        """
        Allocates the next spiral island for player and generates the starter island structures.
        """
        idx = self.next_island_index
        self.next_island_index += 1

        grid = IslandMath.index_to_grid_pos(idx)
        center = IslandMath.index_to_center_pos(idx)

        cx, cz = center.x, center.z
        base_y = 64

        island = SkyblockIsland(
            owner_uuid=player.uuid,
            owner_name=player.name,
            island_index=idx,
            grid_x=grid.u,
            grid_z=grid.v,
            center_x=cx,
            center_z=cz,
            home_x=cx + 0.5,
            home_y=base_y + 1.0,  # 65.0
            home_z=cz + 0.5,
            home_yaw=0.0,
            home_pitch=0.0,
            is_locked=True,
            created_at=int(self.current_time_seconds * 1000)
        )

        # Generate Starter Island Structure
        # 1. Bedrock anchor at (cx, 62, cz)
        island.blocks[(cx, base_y - 2, cz)] = "minecraft:bedrock"

        # 2. Dirt base & Grass top
        for ox in range(-1, 2):
            for oz in range(-1, 2):
                if ox != 0 or oz != 0:
                    island.blocks[(cx + ox, base_y - 2, cz + oz)] = "minecraft:dirt"
                island.blocks[(cx + ox, base_y - 1, cz + oz)] = "minecraft:dirt"
                island.blocks[(cx + ox, base_y, cz + oz)] = "minecraft:grass_block"

        # 3. Oak Tree
        tree_x = cx - 1
        tree_z = cz - 1
        for y in range(1, 6):
            island.blocks[(tree_x, base_y + y, tree_z)] = "minecraft:oak_log"
        # Leaves canopy
        for ox in range(-2, 3):
            for oz in range(-2, 3):
                for oy in (4, 5):
                    if abs(ox) == 2 and abs(oz) == 2:
                        continue
                    pos = (tree_x + ox, base_y + oy, tree_z + oz)
                    if pos not in island.blocks:
                        island.blocks[pos] = "minecraft:oak_leaves"
        for ox in range(-1, 2):
            for oz in range(-1, 2):
                pos = (tree_x + ox, base_y + 6, tree_z + oz)
                if pos not in island.blocks:
                    island.blocks[pos] = "minecraft:oak_leaves"

        # 4. Starter Chest at (cx + 1, 65, cz)
        island.blocks[(cx + 1, base_y + 1, cz)] = "minecraft:chest"
        island.chest_inventory = {
            0: ("minecraft:lava_bucket", 1),
            1: ("minecraft:ice", 2),
            2: ("minecraft:bread", 16),
            3: ("minecraft:oak_sapling", 1),
            4: ("minecraft:bone_meal", 2),
            5: ("minecraft:sugar_cane", 1),
            6: ("minecraft:pumpkin_seeds", 1),
            7: ("minecraft:melon_seeds", 1)
        }

        self.islands[player.uuid] = island
        return island

    def get_or_create_island(self, player: MockPlayer) -> SkyblockIsland:
        if player.uuid in self.islands:
            return self.islands[player.uuid]
        return self.allocate_island(player)

    # ==================== Teleportation & State Persistence ====================

    def cmd_is(self, player: MockPlayer) -> Tuple[bool, str]:
        """
        Handles `/is` command.
        - If outside Skyblock: checks combat and falling cooldowns, records return location, teleports.
        - If inside Skyblock: preserves existing return location, teleports to own home.
        """
        # Outside Skyblock checks
        if player.dimension != self.SKYBLOCK_DIMENSION:
            # 1. Combat cooldown check (5.0 seconds)
            time_since_damage = self.current_time_seconds - player.last_damage_time
            if time_since_damage < IslandMath.COMBAT_COOLDOWN_SECONDS:
                remaining = IslandMath.COMBAT_COOLDOWN_SECONDS - time_since_damage
                msg = f"§cYou cannot teleport while in combat! Cooldown: {remaining:.1f}s remaining."
                player.send_system_message(msg)
                return False, msg

            # 2. Falling check
            if player.fall_distance > 1.5 or (not player.on_ground and player.vy < -0.15):
                msg = "§cYou cannot teleport while falling!"
                player.send_system_message(msg)
                return False, msg

            # 3. Save prior dimension and coordinates (ONLY when entering from outside)
            self.player_returns[player.uuid] = PlayerReturnLocation(
                dimension=player.dimension,
                x=player.x,
                y=player.y,
                z=player.z,
                yaw=player.yaw,
                pitch=player.pitch
            )

        island = self.get_or_create_island(player)
        player.teleport(
            dimension=self.SKYBLOCK_DIMENSION,
            x=island.home_x,
            y=island.home_y,
            z=island.home_z,
            yaw=island.home_yaw,
            pitch=island.home_pitch
        )
        msg = "§aTeleported to your Skyblock island!"
        player.send_system_message(msg)
        return True, msg

    def cmd_return(self, player: MockPlayer) -> Tuple[bool, str]:
        """
        Handles `/return` or `/is return`. Returns player to saved main world position.
        """
        if player.dimension != self.SKYBLOCK_DIMENSION:
            msg = "§cYou are already in the main world!"
            player.send_system_message(msg)
            return False, msg

        return_loc = self.player_returns.get(player.uuid)
        if return_loc is None:
            # Fallback to Overworld world spawn
            player.teleport(self.OVERWORLD_DIMENSION, 0.5, 64.0, 0.5)
            msg = "§6No saved location found. Returned to world spawn."
            player.send_system_message(msg)
            return True, msg

        player.teleport(
            dimension=return_loc.dimension,
            x=return_loc.x,
            y=return_loc.y,
            z=return_loc.z,
            yaw=return_loc.yaw,
            pitch=return_loc.pitch
        )
        msg = f"§aReturned to {return_loc.dimension} at ({return_loc.x:.1f}, {return_loc.y:.1f}, {return_loc.z:.1f})!"
        player.send_system_message(msg)
        return True, msg

    def cmd_sethome(self, player: MockPlayer) -> Tuple[bool, str]:
        """
        Handles `/is sethome`. Customizes island home within 256x256 buildable boundary.
        """
        if player.dimension != self.SKYBLOCK_DIMENSION:
            msg = "§cYou can only set home inside the Skyblock dimension!"
            player.send_system_message(msg)
            return False, msg

        island = self.get_island_by_owner(player.uuid)
        if island is None:
            msg = "§cYou do not have a Skyblock island!"
            player.send_system_message(msg)
            return False, msg

        dx = abs(player.x - island.center_x)
        dz = abs(player.z - island.center_z)
        if dx > IslandMath.ISLAND_RADIUS or dz > IslandMath.ISLAND_RADIUS:
            msg = "§cYou can only set your home within your 256x256 island bounds!"
            player.send_system_message(msg)
            return False, msg

        island.home_x = player.x
        island.home_y = player.y
        island.home_z = player.z
        island.home_yaw = player.yaw
        island.home_pitch = player.pitch
        msg = f"§aIsland home updated to ({player.x:.1f}, {player.y:.1f}, {player.z:.1f})!"
        player.send_system_message(msg)
        return True, msg

    # ==================== Social, Visiting & Permissions ====================

    def cmd_lock(self, player: MockPlayer) -> Tuple[bool, str]:
        island = self.get_island_by_owner(player.uuid)
        if not island:
            msg = "§cYou do not have a Skyblock island!"
            player.send_system_message(msg)
            return False, msg
        island.is_locked = True
        msg = "§6Your island is now locked (invite-only)."
        player.send_system_message(msg)
        return True, msg

    def cmd_unlock(self, player: MockPlayer) -> Tuple[bool, str]:
        island = self.get_island_by_owner(player.uuid)
        if not island:
            msg = "§cYou do not have a Skyblock island!"
            player.send_system_message(msg)
            return False, msg
        island.is_locked = False
        msg = "§aYour island is now unlocked (public visits allowed)."
        player.send_system_message(msg)
        return True, msg

    def cmd_invite(self, player: MockPlayer, target_player: MockPlayer) -> Tuple[bool, str]:
        island = self.get_island_by_owner(player.uuid)
        if not island:
            msg = "§cYou do not have a Skyblock island!"
            player.send_system_message(msg)
            return False, msg
        island.invited_players.add(target_player.uuid)
        msg_owner = f"§aInvited {target_player.name} to your island!"
        msg_target = f"§a{player.name} invited you to their island! Run /visit {player.name} to visit."
        player.send_system_message(msg_owner)
        target_player.send_system_message(msg_target)
        return True, msg_owner

    def cmd_visit(self, visitor: MockPlayer, target_name: str) -> Tuple[bool, str]:
        target_island = self.get_island_by_name(target_name)
        if target_island is None:
            msg = f"§cPlayer '{target_name}' does not have a Skyblock island!"
            visitor.send_system_message(msg)
            return False, msg

        # Check access permission
        is_owner = (visitor.uuid == target_island.owner_uuid)
        is_invited = (visitor.uuid in target_island.invited_players)

        if target_island.is_locked and not is_owner and not is_invited:
            msg = f"§c{target_name}'s island is locked (invite-only). You need an invitation."
            visitor.send_system_message(msg)
            return False, msg

        # Save visitor's return location if entering from outside Skyblock
        if visitor.dimension != self.SKYBLOCK_DIMENSION:
            self.player_returns[visitor.uuid] = PlayerReturnLocation(
                dimension=visitor.dimension,
                x=visitor.x,
                y=visitor.y,
                z=visitor.z,
                yaw=visitor.yaw,
                pitch=visitor.pitch
            )

        visitor.teleport(
            dimension=self.SKYBLOCK_DIMENSION,
            x=target_island.home_x,
            y=target_island.home_y,
            z=target_island.home_z,
            yaw=target_island.home_yaw,
            pitch=target_island.home_pitch
        )
        msg = f"§aVisiting {target_island.owner_name}'s island!"
        visitor.send_system_message(msg)
        return True, msg

    def cmd_kick(self, owner: MockPlayer, target_player: MockPlayer) -> Tuple[bool, str]:
        owner_island = self.get_island_by_owner(owner.uuid)
        if not owner_island:
            msg = "§cYou do not have an island!"
            owner.send_system_message(msg)
            return False, msg

        # Check if target is located within owner's island bounds
        in_bounds = (
            target_player.dimension == self.SKYBLOCK_DIMENSION and
            IslandMath.is_within_island_bounds(
                target_player.x, target_player.z, owner_island.center_x, owner_island.center_z
            )
        )
        if not in_bounds:
            msg = f"§c{target_player.name} is not on your island!"
            owner.send_system_message(msg)
            return False, msg

        # Revoke invitation if present
        owner_island.invited_players.discard(target_player.uuid)

        # Teleport kicked player to their own island home or world return
        target_island = self.get_island_by_owner(target_player.uuid)
        if target_island:
            target_player.teleport(
                self.SKYBLOCK_DIMENSION,
                target_island.home_x,
                target_island.home_y,
                target_island.home_z
            )
        else:
            self.cmd_return(target_player)

        msg_owner = f"§6Kicked {target_player.name} from your island."
        msg_target = f"§cYou were kicked from {owner.name}'s island."
        owner.send_system_message(msg_owner)
        target_player.send_system_message(msg_target)
        return True, msg_owner

    # ==================== Anti-Grief Protection ====================

    def _resolve_island_at(self, x: float, z: float) -> Optional[SkyblockIsland]:
        """Finds which island owns coordinates (x, z), if any."""
        closest_center = IslandMath.get_closest_island_center(x, z)
        for island in self.islands.values():
            if island.center_x == closest_center.x and island.center_z == closest_center.z:
                return island
        return None

    def can_player_interact_block(self, player: MockPlayer, bx: int, by: int, bz: int, action: str) -> bool:
        """
        action: 'break', 'place', 'interact' (containers/doors/buttons).
        Returns True if permitted, False if denied.
        """
        # Outside Skyblock: vanilla rules apply
        if player.dimension != self.SKYBLOCK_DIMENSION:
            return True

        # OP bypass
        if player.is_op:
            return True

        # Void buffer check: nobody can place or break in buffer zone
        if IslandMath.is_buffer_zone(bx, bz):
            return False

        island = self._resolve_island_at(bx, bz)
        if island is None:
            return False

        # Strict 1 owner per island: non-owners cannot modify or access
        return island.owner_uuid == player.uuid

    def can_player_damage_entity(self, attacker: MockPlayer, target_is_player: bool, ex: float, ez: float) -> bool:
        """
        Evaluates melee/projectile attacks against entities or players.
        """
        # Outside Skyblock: vanilla rules apply
        if attacker.dimension != self.SKYBLOCK_DIMENSION:
            return True

        # PvP is completely disabled in Skyblock dimension
        if target_is_player:
            return False

        # OP bypass for entities
        if attacker.is_op:
            return True

        island = self._resolve_island_at(ex, ez)
        if island is None:
            return False

        # Non-owners cannot harm entities on visited islands
        return island.owner_uuid == attacker.uuid

    # ==================== Boundary Containment & Void Rescue ====================

    def tick_player_boundary(self, player: MockPlayer) -> Dict[str, Any]:
        """
        Runs per-tick boundary containment for a player in Skyblock.
        Returns report dict: {'rescued': bool, 'clamped': bool, 'reason': str}
        """
        if player.dimension != self.SKYBLOCK_DIMENSION:
            return {"rescued": False, "clamped": False, "reason": "not_in_skyblock"}

        island = self._resolve_island_at(player.x, player.z)
        # If player is in deep buffer with no known center, use player's own island
        if island is None:
            island = self.get_island_by_owner(player.uuid)
        if island is None:
            return {"rescued": False, "clamped": False, "reason": "no_island"}

        cx, cz = island.center_x, island.center_z

        # 1. Hard Emergency Rescue: deep buffer penetration (> 135 blocks)
        if IslandMath.requires_emergency_rescue(player.x, player.z, cx, cz):
            player.teleport(self.SKYBLOCK_DIMENSION, island.home_x, island.home_y, island.home_z)
            player.fall_distance = 0.0
            player.send_actionbar("§cEmergency Rescue! Returned to island home.")
            return {"rescued": True, "clamped": False, "reason": "emergency_buffer_penetration"}

        # 2. Soft Clamping: island border [-127.5, 127.5]
        new_x, new_z, was_clamped = IslandMath.clamp_position(player.x, player.z, cx, cz)
        if was_clamped:
            player.x = new_x
            player.z = new_z
            player.vx = 0.0
            player.vz = 0.0
            player.send_actionbar("§cYou have reached the island boundary!")
            return {"rescued": False, "clamped": True, "reason": "soft_border_clamp"}

        return {"rescued": False, "clamped": False, "reason": "normal"}

    def tick_void_fall(self, player: MockPlayer) -> bool:
        """
        Runs per-tick void rescue check. Triggered at y < -64.0.
        Returns True if player was rescued from void.
        """
        if player.dimension != self.SKYBLOCK_DIMENSION:
            return False

        if player.y < IslandMath.VOID_RESCUE_THRESHOLD_Y:
            island = self.get_island_by_owner(player.uuid)
            if not island:
                island = self.allocate_island(player)

            player.teleport(self.SKYBLOCK_DIMENSION, island.home_x, island.home_y, island.home_z)
            player.fall_distance = 0.0
            player.vx = 0.0
            player.vy = 0.0
            player.vz = 0.0
            player.send_system_message("§6[Skyblock] §eSaved from the void! Teleported back to your island.")
            return True

        return False

    # ==================== Portal Suppression ====================

    def can_ignite_nether_portal(self, dimension: str) -> bool:
        """Nether portal ignition is blocked inside Skyblock."""
        return dimension != self.SKYBLOCK_DIMENSION

    def can_traverse_nether_portal(self, dimension: str) -> bool:
        """Nether portal traversal is blocked inside Skyblock."""
        return dimension != self.SKYBLOCK_DIMENSION

    def can_activate_end_portal(self, dimension: str) -> bool:
        """End portal frame activation is blocked inside Skyblock."""
        return dimension != self.SKYBLOCK_DIMENSION

    def can_traverse_end_portal(self, dimension: str) -> bool:
        """End portal traversal is blocked inside Skyblock."""
        return dimension != self.SKYBLOCK_DIMENSION

    # ==================== keepInventory & Death Handling ====================

    def handle_player_death(self, player: MockPlayer) -> Dict[str, Any]:
        """
        Processes player death.
        - In Skyblock: keepInventory and XP retention ALWAYS active.
        - Outside Skyblock: follows global_keep_inventory.
        """
        in_skyblock = (player.dimension == self.SKYBLOCK_DIMENSION)
        keep_inventory = in_skyblock or self.global_keep_inventory

        dropped_items = []
        dropped_xp = 0

        if not keep_inventory:
            dropped_items = copy.deepcopy(player.inventory)
            dropped_xp = player.total_xp
            player.inventory = []
            player.xp_level = 0
            player.total_xp = 0
            player.xp_progress = 0.0

        player.is_alive = False

        return {
            "in_skyblock": in_skyblock,
            "keep_inventory_applied": keep_inventory,
            "dropped_items": dropped_items,
            "dropped_xp": dropped_xp
        }

    def handle_player_respawn(self, player: MockPlayer, death_dimension: str) -> None:
        """
        Handles player respawn across dimensions.
        Restores full inventory and XP if death occurred in Skyblock.
        """
        player.is_alive = True
        player.health = 20.0
        player.fall_distance = 0.0

        if death_dimension == self.SKYBLOCK_DIMENSION:
            # Player retains inventory and XP regardless of respawn dimension!
            island = self.get_island_by_owner(player.uuid)
            if island:
                player.teleport(self.SKYBLOCK_DIMENSION, island.home_x, island.home_y, island.home_z)
            else:
                player.teleport(self.OVERWORLD_DIMENSION, 0.5, 64.0, 0.5)
        else:
            # Overworld respawn
            player.teleport(self.OVERWORLD_DIMENSION, 0.5, 64.0, 0.5)

    # ==================== JSON Persistence & Atomic Writes ====================

    def to_json_dict(self) -> Dict[str, Any]:
        return {
            "version": 1,
            "nextIslandIndex": self.next_island_index,
            "islands": {uuid: island.to_dict() for uuid, island in self.islands.items()},
            "playerReturns": {uuid: ret.to_dict() for uuid, ret in self.player_returns.items()}
        }

    def save_to_file(self, target_path: Optional[str] = None) -> str:
        path = target_path or self.world_data_path
        if not path:
            raise ValueError("No target path provided for persistence")

        parent_dir = os.path.dirname(os.path.abspath(path))
        os.makedirs(parent_dir, exist_ok=True)

        data = self.to_json_dict()
        tmp_path = path + ".tmp"

        # Atomic write: write to tmp then replace
        with open(tmp_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

        os.replace(tmp_path, path)
        return path

    def load_from_file(self, source_path: Optional[str] = None) -> None:
        path = source_path or self.world_data_path
        if not path or not os.path.exists(path):
            return

        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        self.next_island_index = data.get("nextIslandIndex", 0)
        self.islands.clear()
        for uuid, island_data in data.get("islands", {}).items():
            self.islands[uuid] = SkyblockIsland.from_dict(island_data)

        self.player_returns.clear()
        for uuid, ret_data in data.get("playerReturns", {}).items():
            self.player_returns[uuid] = PlayerReturnLocation.from_dict(ret_data)
