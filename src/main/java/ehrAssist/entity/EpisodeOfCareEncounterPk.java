package ehrAssist.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeOfCareEncounterPk implements Serializable {
    private UUID episodeId;
    private UUID encounterId;
}
