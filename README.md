# TeDiSync TeamSpeak API

Andere Bungee-Plugins können `TeDiSync#getTeamSpeakApi()` verwenden. Die Schnittstelle
`de.fanta.tedisync.api.TeamSpeakApi` enthält keine Typen der eingebetteten Query-Bibliothek.
TeDiSync als `provided`/`compileOnly` ohne transitive Abhängigkeiten einbinden und als
Soft-Dependency eintragen; TeDiSync oder seine Bibliotheken nicht in das eigene Plugin bündeln.

```java
TeDiSync plugin = (TeDiSync) proxy.getPluginManager().getPlugin("TeDiSync");
TeamSpeakApi api = plugin.getTeamSpeakApi();
if (api.isAvailable()) {
    api.addServerGroup(tsUid, groupId)
        .thenCompose(ignored -> api.setDescription(tsUid, "VoiceChat-User: " + minecraftName))
        .exceptionally(error -> { logger.warning(error.toString()); return null; });
}
// Auch nach dem TS-Disconnect möglich:
api.removeServerGroup(tsUid, groupId);
```

Alle drei Änderungen liefern `CompletableFuture<Void>`. Auf diesen Futures nicht im
Event- oder Audiothread warten. `isAvailable()` meldet eine betriebsbereite Query-Verbindung;
ein nachfolgender Aufruf kann dennoch fehlschlagen. Bei ausgeschaltetem TeamSpeak ist die
API weiterhin vorhanden und liefert fehlgeschlagene Futures.

`tsUid` ist die dauerhafte TeamSpeak-Identität, kein Nickname, keine laufende Client-ID und
keine Minecraft-UUID. Bekannte Offline-Accounts werden ebenfalls unterstützt. Hinzufügen
einer vorhandenen beziehungsweise Entfernen einer fehlenden Gruppenzugehörigkeit ist
erfolgreich. Gruppen-IDs müssen positiv sein. Eine Beschreibung ersetzt den bisherigen
Inhalt, darf leer sein und wird auf höchstens 200 UTF-16-Zeichen gekürzt, ohne ein
Surrogatpaar zu teilen. Ungültige Eingaben, unbekannte Accounts und Query-/Rechtefehler
werden als fehlgeschlagene Futures zurückgegeben. Aufrufe derselben UID werden serialisiert.