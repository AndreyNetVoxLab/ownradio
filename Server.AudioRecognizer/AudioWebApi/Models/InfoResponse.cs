namespace AudioWebApi
{
    public class InfoResponse
    {
        public ResponseStatus Status { get; set; }
        public string Title { get; set; }
        public string Artist { get; set; }
        public bool LimitExceeded { get; set; }

    }


    public enum ResponseStatus
    {
        OK,
        NotFound,
        FileLarge,
        FileSmall,
        LimitExceeded
    }
}