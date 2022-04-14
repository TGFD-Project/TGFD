package ParalleRunner;

public enum Status {
    Coordinator_Waits_For_Workers_Status,
    Coordinator_Waits_For_Workers_Results,
    Coordinator_Assigns_jobs_To_Workers,
    Coordinator_Is_Done,
    Worker_waits_For_Job,
    Worker_Received_Job,
    Worker_Shipping_Data,
    Worker_Receiving_Data,
    Worker_Ready_To_Run,
    Worker_Is_Done
}
