using Microsoft.Extensions.Options;
using NAudio.Wave;
using Newtonsoft.Json;
using Serilog;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace AudioWebApi
{
    public class ACRCloud : IACRCloud
    {
        private ACRSettings _settings;

        public ACRCloud(IOptions<ACRSettings> settings)
        {
            _settings = settings.Value;
        }

        /// <summary>
        /// Обрезаем аудио файл
        /// </summary>
        /// <param name="fileStream">Stream файла</param>
        /// <param name="begin">С секунды</param>
        /// <param name="end">По секунду</param>
        private byte[] TrimMp3(Stream fileStream, TimeSpan? begin, TimeSpan? end)
        {
            

            using (var reader = new Mp3FileReader(fileStream))
            {
                if (reader.TotalTime < TimeSpan.FromSeconds(10))
                {
                    return null;
                }

                if (reader.TotalTime < end)
                {
                    end = reader.TotalTime;
                    begin = end - begin;
                }

                using (var writer = new MemoryStream())
                {
                    Mp3Frame frame;

                    while ((frame = reader.ReadNextFrame()) != null)
                        if (reader.CurrentTime >= begin || !begin.HasValue)
                        {
                            if (reader.CurrentTime <= end || !end.HasValue)
                            {
                                writer.Write(frame.RawData, 0, frame.RawData.Length);
                            }
                            else break;
                        }

                    return writer.ToArray();
                }
            }
        }

        /// <summary>
        /// Получаем краткую информацию о аудио файле
        /// </summary>
        /// <param name="filePath">Путь к аудио файлу</param>
        public InfoResponse GetInfo(Stream fileStream)
        {
            IdentifyProtocolV1 identify = new IdentifyProtocolV1();

            ResponseACRCloud result = null;
            InfoResponse response = new InfoResponse();

           // var data = TrimMp3(fileStream, TimeSpan.FromSeconds(10), TimeSpan.FromSeconds(20)); //этот метод в линуксе не работает

            ///////////////вместо закоментированной строки//////////////
            int length = Convert.ToInt32(fileStream.Length);
            byte[] data = new byte[length / 30];
            fileStream.Read(data, 0, length / 30);
            ///////////////вместо закоментированной строки//////////////

            if (data == null)  // file less 10 seconds
            {
                Log.Warning("File less 10 seconds");
                response.Status = ResponseStatus.FileSmall;
                return response;
            }

            string host = _settings.Host;
            string accessKey = _settings.AccessKey;
            string secretKey = _settings.SecretKey;

            string json = identify.Recognize(host, accessKey, secretKey, data, "audio");


            if (json != "")
                result = JsonConvert.DeserializeObject<ResponseACRCloud>(json);

            if (result != null && result.Status.Message == "Success")
            {
                Log.Information("Audio file found");
                response.Status = ResponseStatus.OK;
                response.Artist = GetArtists(result.Metadata.Musics);
                response.Title = string.Join(",", result.Metadata.Musics.Select(x => x.Title).Distinct());
                response.LimitExceeded = false;
            }
            else
            {
                switch (result.Status.Code)
                {
                    case 1001:
                        Log.Warning("File not found");
                        response.Status = ResponseStatus.NotFound;
                        response.LimitExceeded = false;
                        break;
                    case 3003:
                        Log.Warning("Day limit exceed");
                        response.Status = ResponseStatus.LimitExceeded;
                        response.Artist = "";
                        response.Title = "";
                        response.LimitExceeded = true;
                        break;
                    case 3016:
                        Log.Warning("File is large");
                        response.Status = ResponseStatus.FileLarge;
                        response.LimitExceeded = false;
                        break;
                }
            }

            return response;
        }

        private string GetArtists(List<Music> musics)
        {
            string res = "";

            for (int i = 0; i < musics.Count; i++)
            {
                res += string.Join(",", musics[i].Artists.Select(x => x.Name).Distinct());
            }

            return res;
        }
    }
}