package ehrAssist.util;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class BundleBuilder {

    @Value("${fhir.base.url}")
    private String baseUrl;

    public Bundle searchSet(String resourceType, List<? extends Resource> resources, long total) {
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setMeta(new Meta().setLastUpdated(new Date()));
        bundle.setTotal((int) total);
        bundle.addLink().setRelation("self").setUrl(baseUrl + "/" + resourceType);

        for (Resource resource : resources) {
            bundle.addEntry()
                    .setFullUrl(baseUrl + "/" + resourceType + "/" + resource.getIdElement().getIdPart())
                    .setResource(resource)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }

        return bundle;
    }

    public Bundle searchSetWithPagination(String resourceType, List<? extends Resource> resources, long total,
                                          int page, int size, String queryParams) {
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setMeta(new Meta().setLastUpdated(new Date()));
        bundle.setTotal((int) total);

        String baseQuery = baseUrl + "/" + resourceType + (queryParams != null && !queryParams.isEmpty() ? "?" + queryParams : "");
        bundle.addLink().setRelation("self").setUrl(baseQuery + (queryParams != null && !queryParams.isEmpty() ? "&" : "?") + "_page=" + page + "&_count=" + size);

        if (page > 0) {
            bundle.addLink().setRelation("previous").setUrl(baseQuery + (queryParams != null && !queryParams.isEmpty() ? "&" : "?") + "_page=" + (page - 1) + "&_count=" + size);
        }

        int totalPages = (int) Math.ceil((double) total / size);
        if (page < totalPages - 1) {
            bundle.addLink().setRelation("next").setUrl(baseQuery + (queryParams != null && !queryParams.isEmpty() ? "&" : "?") + "_page=" + (page + 1) + "&_count=" + size);
        }

        for (Resource resource : resources) {
            bundle.addEntry()
                    .setFullUrl(baseUrl + "/" + resourceType + "/" + resource.getIdElement().getIdPart())
                    .setResource(resource)
                    .getSearch().setMode(Bundle.SearchEntryMode.MATCH);
        }

        return bundle;
    }
}
