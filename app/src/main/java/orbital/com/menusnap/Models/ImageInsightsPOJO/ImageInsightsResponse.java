package orbital.com.menusnap.Models.ImageInsightsPOJO;

import java.util.ArrayList;
import java.util.List;

import orbital.com.menusnap.Models.ImageSearchPOJO.Instrumentation;

/**
 * Created by zhiyong on 27/6/2016.
 * For now the modulesRequested parameter is empty. Possible to modify. Check
 * https://msdn.microsoft.com/en-us/library/dn760791.aspx?f=255&MSPPError=-2147217396#modulesrequested
 * Adding all the POJOs for case where modulesRequested=All
 *
 * Check https://msdn.microsoft.com/en-us/library/mt712790.aspx?f=255&MSPPError=-2147217396
 * for example of how to use moduleRequested=All
 */

public class ImageInsightsResponse {

    private String type;
    private Instrumentation instrumentation;
    private BestRepresentativeQuery bestRepresentativeQuery;
    private ImageCaption imageCaption;
    private List<PagesIncluding> pagesIncluding = new ArrayList<PagesIncluding>();
    private List<RelatedCollection> relatedCollections = new ArrayList<RelatedCollection>();
    private ShoppingSources shoppingSources;
    private List<RelatedSearch> relatedSearches = new ArrayList<RelatedSearch>();
    private String imageInsightsToken;

    /**
     *
     * @return
     * The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     * The _type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return
     * The instrumentation
     */
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     *
     * @param instrumentation
     * The instrumentation
     */
    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     *
     * @return
     * The bestRepresentativeQuery
     */
    public BestRepresentativeQuery getBestRepresentativeQuery() {
        return bestRepresentativeQuery;
    }

    /**
     *
     * @param bestRepresentativeQuery
     * The bestRepresentativeQuery
     */
    public void setBestRepresentativeQuery(BestRepresentativeQuery bestRepresentativeQuery) {
        this.bestRepresentativeQuery = bestRepresentativeQuery;
    }

    /**
     *
     * @return
     * The imageCaption
     */
    public ImageCaption getImageCaption() {
        return imageCaption;
    }

    /**
     *
     * @param imageCaption
     * The imageCaption
     */
    public void setImageCaption(ImageCaption imageCaption) {
        this.imageCaption = imageCaption;
    }

    /**
     *
     * @return
     * The pagesIncluding
     */
    public List<PagesIncluding> getPagesIncluding() {
        return pagesIncluding;
    }

    /**
     *
     * @param pagesIncluding
     * The pagesIncluding
     */
    public void setPagesIncluding(List<PagesIncluding> pagesIncluding) {
        this.pagesIncluding = pagesIncluding;
    }

    /**
     *
     * @return
     * The relatedCollections
     */
    public List<RelatedCollection> getRelatedCollections() {
        return relatedCollections;
    }

    /**
     *
     * @param relatedCollections
     * The relatedCollections
     */
    public void setRelatedCollections(List<RelatedCollection> relatedCollections) {
        this.relatedCollections = relatedCollections;
    }

    /**
     *
     * @return
     * The shoppingSources
     */
    public ShoppingSources getShoppingSources() {
        return shoppingSources;
    }

    /**
     *
     * @param shoppingSources
     * The shoppingSources
     */
    public void setShoppingSources(ShoppingSources shoppingSources) {
        this.shoppingSources = shoppingSources;
    }

    /**
     *
     * @return
     * The relatedSearches
     */
    public List<RelatedSearch> getRelatedSearches() {
        return relatedSearches;
    }

    /**
     *
     * @param relatedSearches
     * The relatedSearches
     */
    public void setRelatedSearches(List<RelatedSearch> relatedSearches) {
        this.relatedSearches = relatedSearches;
    }

    /**
     *
     * @return
     * The imageInsightsToken
     */
    public String getImageInsightsToken() {
        return imageInsightsToken;
    }

    /**
     *
     * @param imageInsightsToken
     * The imageInsightsToken
     */
    public void setImageInsightsToken(String imageInsightsToken) {
        this.imageInsightsToken = imageInsightsToken;
    }
}
