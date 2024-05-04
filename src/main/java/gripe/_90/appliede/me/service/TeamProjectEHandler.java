package gripe._90.appliede.me.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;

import gripe._90.appliede.AppliedE;

import cn.leomc.teamprojecte.TPTeam;
import cn.leomc.teamprojecte.TeamChangeEvent;
import cn.leomc.teamprojecte.TeamKnowledgeProvider;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;

class TeamProjectEHandler {
    private final Map<UUID, TPTeam> playersInSharingTeams = new HashMap<>();
    private final Map<TPTeam, Supplier<IKnowledgeProvider>> providersToKeep = new HashMap<>();

    private TeamProjectEHandler() {
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> clear());
        MinecraftForge.EVENT_BUS.addListener((TeamChangeEvent event) -> clear());
    }

    private boolean notSharingEmc(Map.Entry<UUID, Supplier<IKnowledgeProvider>> provider) {
        if (!(provider.getValue().get() instanceof TeamKnowledgeProvider)) {
            return true;
        }

        var uuid = provider.getKey();
        var team = TPTeam.getOrCreateTeam(uuid);

        if (team.isSharingEMC()) {
            playersInSharingTeams.put(uuid, team);
            providersToKeep.putIfAbsent(team, provider.getValue());
            return true;
        }

        return !playersInSharingTeams.containsValue(team) || providersToKeep.containsValue(provider.getValue());
    }

    private void clear() {
        playersInSharingTeams.clear();
        providersToKeep.clear();
    }

    static class Proxy {
        private Object handler;

        Proxy() {
            if (AppliedE.isModLoaded("teamprojecte")) {
                handler = new TeamProjectEHandler();
            }
        }

        boolean notSharingEmc(Map.Entry<UUID, Supplier<IKnowledgeProvider>> provider) {
            return handler == null || ((TeamProjectEHandler) handler).notSharingEmc(provider);
        }

        void clear() {
            if (handler != null) {
                ((TeamProjectEHandler) handler).clear();
            }
        }
    }
}