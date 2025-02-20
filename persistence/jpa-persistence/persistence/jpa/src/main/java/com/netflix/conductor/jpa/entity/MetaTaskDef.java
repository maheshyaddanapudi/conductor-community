@Entity
@Table(name = "meta_task_def")
@Getter
@Setter
public class MetaTaskDef {

    @Id
    private String name;

    @Column(name = "created_on", updatable = false, nullable = false)
    private Instant createdOn = Instant.now();

    @Column(name = "modified_on", nullable = false)
    private Instant modifiedOn = Instant.now();

    @Column(name = "json_data", nullable = false, columnDefinition = "TEXT")
    private String jsonData;
}
